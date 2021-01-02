(ns or.coad.charge
  (:require ["svgpath" :as svgpath]
            [clojure.string :as s]
            [clojure.walk :as walk]
            [or.coad.config :as config]
            [or.coad.division :as division]
            [or.coad.escutcheon :as escutcheon]
            [or.coad.field-environment :as field-environment]
            [or.coad.geometry :as geometry]
            [or.coad.line :as line]
            [or.coad.options :as options]
            [or.coad.position :as position]
            [or.coad.svg :as svg]
            [or.coad.tincture :as tincture]
            [or.coad.util :as util]
            [or.coad.vector :as v]
            [re-frame.core :as rf]))

(def placeholder-regex
  (re-pattern (str "(?i)(" (s/join "|" (vals config/placeholder-colours)) ")")))

(def colour-regex
  (re-pattern (str "(?i)#([a-f0-9]{6}|[a-f0-9]{3})")))

(defn find-charge [charge-map [group & rest]]
  (let [next (get-in charge-map [:groups group])]
    (if rest
      (recur next rest)
      next)))

(defn get-charge-map []
  @(rf/subscribe [:load-data "data/charge-map.edn"]))

(defn get-charge-variant-data [{:keys [type attitude variant]}]
  (when-let [charge-map (get-charge-map)]
    (let [lookup-path       (get-in charge-map
                                    [:lookup type])
          charge-data       (get-in (find-charge charge-map lookup-path)
                                    [:charges type])
          attitude-variants (get-in charge-data
                                    [:attitudes attitude :variants])
          variants          (or attitude-variants
                                (:variants charge-data))]
      (get variants variant))))

(defn pick-placeholder-tincture [match {:keys [primary] :as tincture}]
  (let [lower-case-match (s/lower-case match)
        reverse-lookup   (into {} (map (fn [[key value]]
                                         [(s/lower-case value) key])
                                       config/placeholder-colours))
        kind             (get reverse-lookup lower-case-match)]
    (or (get tincture kind)
        primary)))

(defn replace-placeholder-colours [string tincture]
  (s/replace string placeholder-regex
             (fn [[_ match]]
               (pick-placeholder-tincture match tincture))))

(defn replace-non-placeholder-colour [current colour unwanted-placeholder-colours]
  (let [match (s/lower-case current)]
    (if (and (get config/placeholder-colours-set match)
             (not (get unwanted-placeholder-colours match)))
      current
      colour)))

(defn split-style-value [value]
  (-> value
      (s/split #";")
      (->>
       (map (fn [chunk]
              (-> chunk
                  (s/split #":" 2)
                  (as-> [key value]
                      [(keyword (s/trim key)) (s/trim value)])))))
      (into {})))

(defn fix-string-style-values [data]
  (walk/postwalk #(if (and (vector? %)
                           (-> % count (= 2))
                           (-> % first (= :style))
                           (-> % second string?))
                    [:style (split-style-value (second %))]
                    %)
                 data))

(defn replace-placeholder-colours-everywhere [data tincture]
  (walk/postwalk #(if (string? %)
                    (replace-placeholder-colours % tincture)
                    %)
                 data))

(defn replace-non-placeholder-colours-everywhere [data colour unwanted-placeholder-colours]
  (walk/postwalk #(if (and (vector? %)
                           (-> % second string?)
                           (->> % first (get #{:stroke :fill}))
                           (-> % second (not= "none")))
                    [(first %) (replace-non-placeholder-colour
                                (second %)
                                colour
                                unwanted-placeholder-colours)]
                    %)
                 data))

(defn remove-outlines [data]
  (walk/postwalk #(if (and (vector? %)
                           (->> % first (get #{:stroke :fill}))
                           (->> % second (get #{"#000000" "#000" "black"})))
                    [(first %) "none"]
                    %)
                 data))

(defn make-mask [data provided-placeholder-colours]
  (let [mask-id                      (util/id "mask")
        mask-inverted-id             (util/id "mask")
        unwanted-placeholder-colours (-> provided-placeholder-colours
                                         (dissoc :primary)
                                         (->>
                                          (filter second)
                                          (map (fn [[k _]]
                                                 (get config/placeholder-colours k)))
                                          set))
        mask                         (-> data
                                         (replace-non-placeholder-colours-everywhere
                                          "#fff" unwanted-placeholder-colours)
                                         (replace-placeholder-colours-everywhere {:primary "#000"}))
        mask-inverted                (-> data
                                         remove-outlines
                                         (replace-non-placeholder-colours-everywhere
                                          "#000" unwanted-placeholder-colours)
                                         (replace-placeholder-colours-everywhere {:primary "#fff"}))]
    [mask-id mask mask-inverted-id mask-inverted]))

(defn counterchange-field [field {:keys [division]}]
  (let [type (:type division)]
    (-> field
        (dissoc :content)
        (assoc :division {:type   type
                          :line   (:line division)
                          :fields (-> (division/default-fields type)
                                      (assoc-in [0 :content :tincture] (get-in division [:fields 1 :content :tincture]))
                                      (assoc-in [1 :content :tincture] (get-in division [:fields 0 :content :tincture])))}))))

(defn counterchangable? [field parent]
  (and (:counterchanged? field)
       (division/counterchangable? (-> parent :division))))

(def default-options
  {:position   position/default-options
   :geometry   geometry/default-options
   :escutcheon {:type    :choice
                :choices escutcheon/choices
                :default :heater}})

(defn options [charge]
  (when charge
    (let [type (:type charge)]
      (->
       default-options
       (options/merge
        (->
         (get {:escutcheon {:geometry {:size      {:default 30}
                                       :mirrored? nil
                                       :reversed? nil}}
               :roundel    {:geometry {:mirrored? nil
                                       :reversed? nil}}
               :annulet    {:geometry {:mirrored? nil
                                       :reversed? nil}}
               :billet     {:geometry {:mirrored? nil
                                       :reversed? nil}}
               :lozenge    {:geometry {:mirrored? nil
                                       :reversed? nil}}
               :fusil      {:geometry {:mirrored? nil
                                       :reversed? nil}}
               :mascle     {:geometry {:mirrored? nil
                                       :reversed? nil}}
               :rustre     {:geometry {:mirrored? nil
                                       :reversed? nil}}
               :crescent   {:geometry {:mirrored? nil}}}
              type)
         (cond->
             (not= type :escutcheon) (assoc :escutcheon nil))))))))

(defn escutcheon
  {:display-name "Escutcheon"}
  [{:keys [field hints] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [{:keys [position geometry escutcheon]} (options/sanitize ordinary (options ordinary))
        {:keys [size stretch rotation]}        geometry
        position-point                         (position/calculate position environment :fess)
        width                                  (:width environment)
        ordinary-width                         (-> width
                                                   (* size)
                                                   (/ 100))
        env                                    (field-environment/transform-to-width
                                                (escutcheon/field escutcheon) ordinary-width)
        env-fess                               (-> env :points :fess)
        [min-x max-x min-y max-y]              (svg/rotated-bounding-box (-> env :points :top-left)
                                                                         (-> env :points :bottom-right)
                                                                         rotation
                                                                         :middle env-fess
                                                                         :scale (v/v 1 stretch))
        box-size                               (v/v (- max-x min-x)
                                                    (- max-y min-y))
        env-shape                              (-> (line/translate (:shape env)
                                                                   (-> env-fess :x -)
                                                                   (-> env-fess :y -))
                                                   (cond->
                                                       (not= stretch 1) (->
                                                                         (svgpath)
                                                                         (.scale 1 stretch)
                                                                         (.toString))
                                                       (:squiggly? render-options) line/squiggly-path
                                                       (not= rotation 0)           (->
                                                                                    (svgpath)
                                                                                    (.rotate rotation)
                                                                                    (.toString)))
                                                   (line/translate (:x position-point) (:y position-point)))
        parts                                  [[env-shape
                                                 [(v/- position-point
                                                       (v// box-size 2))
                                                  (v/+ position-point
                                                       (v// box-size 2))]]]
        field                                  (if (counterchangable? field parent)
                                                 (counterchange-field field parent)
                                                 field)]
    [division/make-division
     :ordinary-pale [field] parts
     [:all]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g.outline
        [:path {:d env-shape}]])
     environment ordinary top-level-render render-options :db-path db-path]))

(defn roundel
  {:display-name "Roundel"}
  [{:keys [field hints] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [{:keys [position geometry]}     (options/sanitize ordinary (options ordinary))
        {:keys [size stretch rotation]} geometry
        position-point                  (position/calculate position environment :fess)
        width                           (:width environment)
        ordinary-width                  (-> width
                                            (* size)
                                            (/ 100))
        ordinary-width-half             (/ ordinary-width 2)
        ordinary-shape                  (-> ["m" (v/v ordinary-width-half 0)
                                             ["a" ordinary-width-half ordinary-width-half
                                              0 0 0 (v/v (- ordinary-width) 0)]
                                             ["a" ordinary-width-half ordinary-width-half
                                              0 0 0 ordinary-width 0]
                                             "z"]
                                            svg/make-path
                                            (cond->
                                                (not= stretch 1) (->
                                                                  (svgpath)
                                                                  (.scale 1 stretch)
                                                                  (.toString))
                                                (:squiggly? render-options) line/squiggly-path
                                                (not= rotation 0)           (->
                                                                             (svgpath)
                                                                             (.rotate rotation)
                                                                             (.toString)))
                                            (line/translate (:x position-point) (:y position-point)))
        [min-x max-x min-y max-y]       (svg/rotated-bounding-box (v/-
                                                                   (v/v ordinary-width-half
                                                                        ordinary-width-half))
                                                                  (v/-
                                                                   (v/v ordinary-width-half
                                                                        ordinary-width-half))
                                                                  rotation
                                                                  :scale (v/v 1 stretch))
        box-size                        (v/v (- max-x min-x)
                                             (- max-y min-y))
        parts                           [[ordinary-shape
                                          [(v/- position-point
                                                (v// box-size 2))
                                           (v/+ position-point
                                                (v// box-size 2))]]]
        field                           (if (counterchangable? field parent)
                                          (counterchange-field field parent)
                                          field)]
    [division/make-division
     :ordinary-pale [field] parts
     [:all]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g.outline
        [:path {:d ordinary-shape}]])
     environment ordinary top-level-render render-options :db-path db-path]))

(defn annulet
  {:display-name "Annulet"}
  [{:keys [field hints] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [{:keys [position geometry]}     (options/sanitize ordinary (options ordinary))
        {:keys [size stretch rotation]} geometry
        position-point                  (position/calculate position environment :fess)
        width                           (:width environment)
        ordinary-width                  (-> width
                                            (* size)
                                            (/ 100))
        ordinary-width-half             (/ ordinary-width 2)
        ordinary-shape                  (-> ["m" (v/v ordinary-width-half 0)
                                             ["a" ordinary-width-half ordinary-width-half
                                              0 0 0 (v/v (- ordinary-width) 0)]
                                             ["a" ordinary-width-half ordinary-width-half
                                              0 0 0 ordinary-width 0]
                                             "z"]
                                            svg/make-path
                                            (cond->
                                                (not= stretch 1) (->
                                                                  (svgpath)
                                                                  (.scale 1 stretch)
                                                                  (.toString))
                                                (:squiggly? render-options) line/squiggly-path
                                                (not= rotation 0)           (->
                                                                             (svgpath)
                                                                             (.rotate rotation)
                                                                             (.toString)))
                                            (line/translate (:x position-point) (:y position-point)))
        hole-width                      (* ordinary-width 0.6)
        hole-width-half                 (/ hole-width 2)
        hole-shape                      (-> ["m" (v/v hole-width-half 0)
                                             ["a" hole-width-half hole-width-half
                                              0 0 0 (v/v (- hole-width) 0)]
                                             ["a" hole-width-half hole-width-half
                                              0 0 0 hole-width 0]
                                             "z"]
                                            svg/make-path
                                            (cond->
                                                (not= stretch 1) (->
                                                                  (svgpath)
                                                                  (.scale 1 stretch)
                                                                  (.toString))
                                                (:squiggly? render-options) line/squiggly-path
                                                (not= rotation 0)           (->
                                                                             (svgpath)
                                                                             (.rotate rotation)
                                                                             (.toString)))
                                            (line/translate (:x position-point) (:y position-point)))
        [min-x max-x min-y max-y]       (svg/rotated-bounding-box (v/-
                                                                   (v/v ordinary-width-half
                                                                        ordinary-width-half))
                                                                  (v/-
                                                                   (v/v ordinary-width-half
                                                                        ordinary-width-half))
                                                                  rotation
                                                                  :scale (v/v 1 stretch))
        box-size                        (v/v (- max-x min-x)
                                             (- max-y min-y))
        parts                           [[ordinary-shape
                                          [(v/- position-point
                                                (v// box-size 2))
                                           (v/+ position-point
                                                (v// box-size 2))]
                                          hole-shape]]
        field                           (if (counterchangable? field parent)
                                          (counterchange-field field parent)
                                          field)]
    [division/make-division
     :ordinary-pale [field] parts
     [:all]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g.outline
        [:path {:d ordinary-shape}]
        [:path {:d hole-shape}]])
     environment ordinary top-level-render render-options :db-path db-path]))

(defn billet
  {:display-name "Billet"}
  [{:keys [field hints] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [{:keys [position geometry]}     (options/sanitize ordinary (options ordinary))
        {:keys [size stretch rotation]} geometry
        position-point                  (position/calculate position environment :fess)
        height                          (:height environment)
        ordinary-height                 (-> height
                                            (* size)
                                            (/ 100))
        ordinary-width                  (/ ordinary-height 2)
        ordinary-width-half             (/ ordinary-width 2)
        ordinary-height-half            (/ ordinary-height 2)
        ordinary-shape                  (-> ["m" (v/v (- ordinary-width-half) (- ordinary-height-half))
                                             "h" ordinary-width
                                             "v" ordinary-height
                                             "h" (- ordinary-width)
                                             "z"]
                                            svg/make-path
                                            (cond->
                                                (not= stretch 1) (->
                                                                  (svgpath)
                                                                  (.scale 1 stretch)
                                                                  (.toString))
                                                (:squiggly? render-options) line/squiggly-path
                                                (not= rotation 0)           (->
                                                                             (svgpath)
                                                                             (.rotate rotation)
                                                                             (.toString)))
                                            (line/translate (:x position-point) (:y position-point)))
        [min-x max-x min-y max-y]       (svg/rotated-bounding-box (v/-
                                                                   (v/v ordinary-width-half
                                                                        ordinary-width-half))
                                                                  (v/-
                                                                   (v/v ordinary-width-half
                                                                        ordinary-width-half))
                                                                  rotation
                                                                  :scale (v/v 1 stretch))
        box-size                        (v/v (- max-x min-x)
                                             (- max-y min-y))
        parts                           [[ordinary-shape
                                          [(v/- position-point
                                                (v// box-size 2))
                                           (v/+ position-point
                                                (v// box-size 2))]]]
        field                           (if (counterchangable? field parent)
                                          (counterchange-field field parent)
                                          field)]
    [division/make-division
     :ordinary-pale [field] parts
     [:all]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g.outline
        [:path {:d ordinary-shape}]])
     environment ordinary top-level-render render-options :db-path db-path]))

(defn lozenge
  {:display-name "Lozenge"}
  [{:keys [field hints] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [{:keys [position geometry]}     (options/sanitize ordinary (options ordinary))
        {:keys [size stretch rotation]} geometry
        position-point                  (position/calculate position environment :fess)
        height                          (:height environment)
        ordinary-height                 (-> height
                                            (* size)
                                            (/ 100))
        ordinary-width                  (/ ordinary-height 1.3)
        ordinary-width-half             (/ ordinary-width 2)
        ordinary-height-half            (/ ordinary-height 2)
        ordinary-shape                  (-> ["m" (v/v 0 (- ordinary-height-half))
                                             "l" (v/v ordinary-width-half ordinary-height-half)
                                             "l " (v/v (- ordinary-width-half) ordinary-height-half)
                                             "l" (v/v (- ordinary-width-half) (- ordinary-height-half))
                                             "z"]
                                            svg/make-path
                                            (cond->
                                                (not= stretch 1) (->
                                                                  (svgpath)
                                                                  (.scale 1 stretch)
                                                                  (.toString))
                                                (:squiggly? render-options) line/squiggly-path
                                                (not= rotation 0)           (->
                                                                             (svgpath)
                                                                             (.rotate rotation)
                                                                             (.toString)))
                                            (line/translate (:x position-point) (:y position-point)))
        [min-x max-x min-y max-y]       (svg/rotated-bounding-box (v/-
                                                                   (v/v ordinary-width-half
                                                                        ordinary-width-half))
                                                                  (v/-
                                                                   (v/v ordinary-width-half
                                                                        ordinary-width-half))
                                                                  rotation
                                                                  :scale (v/v 1 stretch))
        box-size                        (v/v (- max-x min-x)
                                             (- max-y min-y))
        parts                           [[ordinary-shape
                                          [(v/- position-point
                                                (v// box-size 2))
                                           (v/+ position-point
                                                (v// box-size 2))]]]
        field                           (if (counterchangable? field parent)
                                          (counterchange-field field parent)
                                          field)]
    [division/make-division
     :ordinary-pale [field] parts
     [:all]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g.outline
        [:path {:d ordinary-shape}]])
     environment ordinary top-level-render render-options :db-path db-path]))

(defn fusil
  {:display-name "Fusil"}
  [{:keys [field hints] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [{:keys [position geometry]}     (options/sanitize ordinary (options ordinary))
        {:keys [size stretch rotation]} geometry
        position-point                  (position/calculate position environment :fess)
        height                          (:height environment)
        ordinary-height                 (-> height
                                            (* size)
                                            (/ 100))
        ordinary-width                  (/ ordinary-height 2)
        ordinary-width-half             (/ ordinary-width 2)
        ordinary-height-half            (/ ordinary-height 2)
        ordinary-shape                  (-> ["m" (v/v 0 (- ordinary-height-half))
                                             "l" (v/v ordinary-width-half ordinary-height-half)
                                             "l " (v/v (- ordinary-width-half) ordinary-height-half)
                                             "l" (v/v (- ordinary-width-half) (- ordinary-height-half))
                                             "z"]
                                            svg/make-path
                                            (cond->
                                                (not= stretch 1) (->
                                                                  (svgpath)
                                                                  (.scale 1 stretch)
                                                                  (.toString))
                                                (:squiggly? render-options) line/squiggly-path
                                                (not= rotation 0)           (->
                                                                             (svgpath)
                                                                             (.rotate rotation)
                                                                             (.toString)))
                                            (line/translate (:x position-point) (:y position-point)))
        [min-x max-x min-y max-y]       (svg/rotated-bounding-box (v/-
                                                                   (v/v ordinary-width-half
                                                                        ordinary-width-half))
                                                                  (v/-
                                                                   (v/v ordinary-width-half
                                                                        ordinary-width-half))
                                                                  rotation
                                                                  :scale (v/v 1 stretch))
        box-size                        (v/v (- max-x min-x)
                                             (- max-y min-y))
        parts                           [[ordinary-shape
                                          [(v/- position-point
                                                (v// box-size 2))
                                           (v/+ position-point
                                                (v// box-size 2))]]]
        field                           (if (counterchangable? field parent)
                                          (counterchange-field field parent)
                                          field)]
    [division/make-division
     :ordinary-pale [field] parts
     [:all]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g.outline
        [:path {:d ordinary-shape}]])
     environment ordinary top-level-render render-options :db-path db-path]))

(defn mascle
  {:display-name "Mascle"}
  [{:keys [field hints] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [{:keys [position geometry]}     (options/sanitize ordinary (options ordinary))
        {:keys [size stretch rotation]} geometry
        position-point                  (position/calculate position environment :fess)
        height                          (:height environment)
        ordinary-height                 (-> height
                                            (* size)
                                            (/ 100))
        ordinary-width                  (/ ordinary-height 1.3)
        ordinary-width-half             (/ ordinary-width 2)
        ordinary-height-half            (/ ordinary-height 2)
        ordinary-shape                  (-> ["m" (v/v 0 (- ordinary-height-half))
                                             "l" (v/v ordinary-width-half ordinary-height-half)
                                             "l " (v/v (- ordinary-width-half) ordinary-height-half)
                                             "l" (v/v (- ordinary-width-half) (- ordinary-height-half))
                                             "z"]
                                            svg/make-path
                                            (cond->
                                                (not= stretch 1) (->
                                                                  (svgpath)
                                                                  (.scale 1 stretch)
                                                                  (.toString))
                                                (:squiggly? render-options) line/squiggly-path
                                                (not= rotation 0)           (->
                                                                             (svgpath)
                                                                             (.rotate rotation)
                                                                             (.toString)))
                                            (line/translate (:x position-point) (:y position-point)))
        hole-width                      (* ordinary-width 0.55)
        hole-height                     (* ordinary-height 0.55)
        hole-width-half                 (/ hole-width 2)
        hole-height-half                (/ hole-height 2)
        hole-shape                      (-> ["m" (v/v 0 (- hole-height-half))
                                             "l" (v/v hole-width-half hole-height-half)
                                             "l " (v/v (- hole-width-half) hole-height-half)
                                             "l" (v/v (- hole-width-half) (- hole-height-half))
                                             "z"]
                                            svg/make-path
                                            (cond->
                                                (not= stretch 1) (->
                                                                  (svgpath)
                                                                  (.scale 1 stretch)
                                                                  (.toString))
                                                (:squiggly? render-options) line/squiggly-path
                                                (not= rotation 0)           (->
                                                                             (svgpath)
                                                                             (.rotate rotation)
                                                                             (.toString)))
                                            (line/translate (:x position-point) (:y position-point)))
        [min-x max-x min-y max-y]       (svg/rotated-bounding-box (v/-
                                                                   (v/v ordinary-width-half
                                                                        ordinary-width-half))
                                                                  (v/-
                                                                   (v/v ordinary-width-half
                                                                        ordinary-width-half))
                                                                  rotation
                                                                  :scale (v/v 1 stretch))
        box-size                        (v/v (- max-x min-x)
                                             (- max-y min-y))
        parts                           [[ordinary-shape
                                          [(v/- position-point
                                                (v// box-size 2))
                                           (v/+ position-point
                                                (v// box-size 2))]
                                          hole-shape]]
        field                           (if (counterchangable? field parent)
                                          (counterchange-field field parent)
                                          field)]
    [division/make-division
     :ordinary-pale [field] parts
     [:all]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g.outline
        [:path {:d ordinary-shape}]
        [:path {:d hole-shape}]])
     environment ordinary top-level-render render-options :db-path db-path]))

(defn rustre
  {:display-name "Rustre"}
  [{:keys [field hints] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [{:keys [position geometry]}     (options/sanitize ordinary (options ordinary))
        {:keys [size stretch rotation]} geometry
        position-point                  (position/calculate position environment :fess)
        height                          (:height environment)
        ordinary-height                 (-> height
                                            (* size)
                                            (/ 100))
        ordinary-width                  (/ ordinary-height 1.3)
        ordinary-width-half             (/ ordinary-width 2)
        ordinary-height-half            (/ ordinary-height 2)
        ordinary-shape                  (-> ["m" (v/v 0 (- ordinary-height-half))
                                             "l" (v/v ordinary-width-half ordinary-height-half)
                                             "l " (v/v (- ordinary-width-half) ordinary-height-half)
                                             "l" (v/v (- ordinary-width-half) (- ordinary-height-half))
                                             "z"]
                                            svg/make-path
                                            (cond->
                                                (not= stretch 1) (->
                                                                  (svgpath)
                                                                  (.scale 1 stretch)
                                                                  (.toString))
                                                (:squiggly? render-options) line/squiggly-path
                                                (not= rotation 0)           (->
                                                                             (svgpath)
                                                                             (.rotate rotation)
                                                                             (.toString)))
                                            (line/translate (:x position-point) (:y position-point)))
        hole-width                      (* ordinary-width 0.5)
        hole-width-half                 (/ hole-width 2)
        hole-shape                      (-> ["m" (v/v hole-width-half 0)
                                             ["a" hole-width-half hole-width-half
                                              0 0 0 (v/v (- hole-width) 0)]
                                             ["a" hole-width-half hole-width-half
                                              0 0 0 hole-width 0]
                                             "z"]
                                            svg/make-path
                                            (cond->
                                                (not= stretch 1) (->
                                                                  (svgpath)
                                                                  (.scale 1 stretch)
                                                                  (.toString))
                                                (:squiggly? render-options) line/squiggly-path
                                                (not= rotation 0)           (->
                                                                             (svgpath)
                                                                             (.rotate rotation)
                                                                             (.toString)))
                                            (line/translate (:x position-point) (:y position-point)))
        [min-x max-x min-y max-y]       (svg/rotated-bounding-box (v/-
                                                                   (v/v ordinary-width-half
                                                                        ordinary-width-half))
                                                                  (v/-
                                                                   (v/v ordinary-width-half
                                                                        ordinary-width-half))
                                                                  rotation
                                                                  :scale (v/v 1 stretch))
        box-size                        (v/v (- max-x min-x)
                                             (- max-y min-y))
        parts                           [[ordinary-shape
                                          [(v/- position-point
                                                (v// box-size 2))
                                           (v/+ position-point
                                                (v// box-size 2))]
                                          hole-shape]]
        field                           (if (counterchangable? field parent)
                                          (counterchange-field field parent)
                                          field)]
    [division/make-division
     :ordinary-pale [field] parts
     [:all]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g.outline
        [:path {:d ordinary-shape}]
        [:path {:d hole-shape}]])
     environment ordinary top-level-render render-options :db-path db-path]))

(defn crescent
  {:display-name "Crescent"}
  [{:keys [field hints] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [{:keys [position geometry]}     (options/sanitize ordinary (options ordinary))
        {:keys [size stretch rotation]} geometry
        position-point                  (position/calculate position environment :fess)
        width                           (:width environment)
        ordinary-width                  (-> width
                                            (* size)
                                            (/ 100))
        ordinary-width-half             (/ ordinary-width 2)
        inner-radius                    (* ordinary-width-half
                                           0.75)
        horn-angle                      -45
        horn-point-x                    (* ordinary-width-half
                                           (-> horn-angle
                                               (* Math/PI)
                                               (/ 180)
                                               Math/cos))
        horn-point-y                    (* ordinary-width-half
                                           (-> horn-angle
                                               (* Math/PI)
                                               (/ 180)
                                               Math/sin))
        horn-point-1                    (v/v horn-point-x horn-point-y)
        horn-point-2                    (v/v (- horn-point-x) horn-point-y)
        ordinary-shape                  (-> ["m" horn-point-1
                                             ["a" ordinary-width-half ordinary-width-half
                                              0 1 1 (v/- horn-point-2 horn-point-1)]
                                             ["a" inner-radius inner-radius
                                              0 1 0 (v/- horn-point-1 horn-point-2)]
                                             "z"]
                                            svg/make-path
                                            (cond->
                                                (not= stretch 1) (->
                                                                  (svgpath)
                                                                  (.scale 1 stretch)
                                                                  (.toString))
                                                (:squiggly? render-options) line/squiggly-path
                                                (not= rotation 0)           (->
                                                                             (svgpath)
                                                                             (.rotate rotation)
                                                                             (.toString)))
                                            (line/translate (:x position-point) (:y position-point)))
        [min-x max-x min-y max-y]       (svg/rotated-bounding-box (v/-
                                                                   (v/v ordinary-width-half
                                                                        ordinary-width-half))
                                                                  (v/-
                                                                   (v/v ordinary-width-half
                                                                        ordinary-width-half))
                                                                  rotation
                                                                  :scale (v/v 1 stretch))
        box-size                        (v/v (- max-x min-x)
                                             (- max-y min-y))
        parts                           [[ordinary-shape
                                          [(v/- position-point
                                                (v// box-size 2))
                                           (v/+ position-point
                                                (v// box-size 2))]]]
        field                           (if (counterchangable? field parent)
                                          (counterchange-field field parent)
                                          field)]
    [division/make-division
     :ordinary-pale [field] parts
     [:all]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g.outline
        [:path {:d ordinary-shape}]])
     environment ordinary top-level-render render-options :db-path db-path]))

(def charges
  [#'roundel
   #'annulet
   #'billet
   #'escutcheon
   #'lozenge
   #'fusil
   #'mascle
   #'rustre
   #'crescent])

(def kinds-function-map
  (->> charges
       (map (fn [function]
              [(-> function meta :name keyword) function]))
       (into {})))

(def choices
  (->> charges
       (map (fn [function]
              [(-> function meta :display-name) (-> function meta :name keyword)]))))

(defn render-other-charge [{:keys [type field tincture hints] :as charge} parent
                           environment top-level-render render-options & {:keys [db-path]}]
  (if-let [charge-data-path (-> charge
                                get-charge-variant-data
                                :path)]
    (if-let [data @(rf/subscribe [:load-data charge-data-path])]
      (let [{:keys [position geometry]}      (options/sanitize charge (options charge))
            {:keys [size stretch
                    mirrored? reversed?
                    rotation]}               geometry
            ;; since size now is filled with a default, check whether it was set at all,
            ;; if not, then use nil
            ;; TODO: this probably needs a better mechanism and form representation
            size                             (if (-> charge :geometry :size) size nil)
            data                             (first data)
            points                           (:points environment)
            top                              (:top points)
            bottom                           (:bottom points)
            left                             (:left points)
            right                            (:right points)
            meta                             (get data 1)
            positional-charge-width          (js/parseFloat (:width meta))
            positional-charge-height         (js/parseFloat (:height meta))
            width                            (:width environment)
            height                           (:height environment)
            center-point                     (position/calculate position environment :fess)
            min-x-distance                   (min (- (:x center-point) (:x left))
                                                  (- (:x right) (:x center-point)))
            min-y-distance                   (min (- (:y center-point) (:y top))
                                                  (- (:y bottom) (:y center-point)))
            target-width                     (if size
                                               (-> size
                                                   (* width)
                                                   (/ 100))
                                               (* (* min-x-distance 2) 0.8))
            target-height                    (/ (if size
                                                  (-> size
                                                      (* height)
                                                      (/ 100))
                                                  (* (* min-y-distance 2) 0.7))
                                                stretch)
            scale-x                          (* (if mirrored? -1 1)
                                                (min (/ target-width positional-charge-width)
                                                     (/ target-height positional-charge-height)))
            scale-y                          (* (if reversed? -1 1)
                                                (* (Math/abs scale-x) stretch))
            adjusted-charge                  (-> data
                                                 fix-string-style-values
                                                 (cond->
                                                     (not (or (:outline? hints)
                                                              (:outline? render-options))) remove-outlines
                                                     (and (:squiggly? render-options)
                                                          (get #{:roundel
                                                                 :fusil
                                                                 :billet} type)) line/squiggly-paths)
                                                 (assoc 0 :g))
            provided-placeholder-colours     (-> {}
                                                 (into (map (fn [[key value]]
                                                              [key (tincture/pick value render-options)])
                                                            (into {}
                                                                  (filter (fn [[_ v]]
                                                                            (not= v :none)) tincture))))
                                                 (assoc :primary "none"))
            [mask-id mask
             mask-inverted-id mask-inverted] (make-mask adjusted-charge provided-placeholder-colours)
            coloured-charge                  (replace-placeholder-colours-everywhere
                                              adjusted-charge
                                              provided-placeholder-colours)
            clip-path-id                     (util/id "clip-path")
            shift                            (-> (v/v positional-charge-width positional-charge-height)
                                                 (v// 2)
                                                 (v/-))
            [min-x max-x min-y max-y]        (svg/rotated-bounding-box
                                              shift
                                              (v/dot shift (v/v -1 -1))
                                              rotation
                                              :scale (v/v scale-x scale-y))
            clip-size                        (v/v (- max-x min-x) (- max-y min-y))
            position                         (-> clip-size
                                                 (v/-)
                                                 (v// 2)
                                                 (v/+ center-point))
            charge-environment               (field-environment/create
                                              (svg/make-path ["M" position
                                                              "l" (v/v (:x clip-size) 0)
                                                              "l" (v/v 0 (:y clip-size))
                                                              "l" (v/v (- (:x clip-size)) 0)
                                                              "l" (v/v 0 (- (:y clip-size)))
                                                              "z"])
                                              {:parent               field
                                               :context              [:charge]
                                               :bounding-box         (svg/bounding-box
                                                                      [position (v/+ position
                                                                                     clip-size)])
                                               :override-environment (when (or (:inherit-environment? field)
                                                                               (counterchangable? field parent)) environment)})
            field                            (if (counterchangable? field parent)
                                               (counterchange-field field parent)
                                               field)]
        [:<>
         [:defs
          [:mask {:id mask-id}
           mask]
          [:mask {:id mask-inverted-id}
           mask-inverted]
          [:clipPath {:id clip-path-id}
           [:rect {:x      0
                   :y      0
                   :width  positional-charge-width
                   :height positional-charge-height
                   :fill   "#fff"}]]]
         (let [transform         (str "translate(" (:x center-point) "," (:y center-point) ")"
                                      "rotate(" rotation ")"
                                      "scale(" scale-x "," scale-y ")"
                                      "translate(" (-> shift :x) "," (-> shift :y) ")")
               reverse-transform (str "translate(" (-> shift :x -) "," (-> shift :y -) ")"
                                      "scale(" (/ 1 scale-x) "," (/ 1 scale-y) ")"
                                      "rotate(" (- rotation) ")"
                                      "translate(" (- (:x center-point)) "," (- (:y center-point)) ")")]
           [:g {:transform transform
                :clip-path (str "url(#" clip-path-id ")")}
            [:g {:mask (str "url(#" mask-inverted-id ")")}
             [:g {:transform reverse-transform}
              [top-level-render field charge-environment render-options :db-path (conj db-path :field)]]]
            [:g {:mask (str "url(#" mask-id ")")}
             coloured-charge]])])
      [:<>])
    [:<>]))

(defn render [{:keys [type] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [function (get kinds-function-map type)]
    (if function
      [function ordinary parent environment top-level-render render-options :db-path db-path]
      [render-other-charge ordinary parent environment top-level-render render-options :db-path db-path])))
