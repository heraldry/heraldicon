(ns or.coad.charge
  (:require [clojure.string :as s]
            [clojure.walk :as walk]
            [or.coad.config :as config]
            [or.coad.division :as division]
            [or.coad.field-environment :as field-environment]
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

(defn squiggly-paths [data]
  (walk/postwalk #(cond-> %
                    (vector? %) ((fn [v]
                                   (if (= (first v) :d)
                                     [:d (line/squiggly-path (second v))]
                                     v))))
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
       (division/counterchangable? (-> parent :division :type))))

(def default-options
  {:position  position/default-options
   :size      {:type    :range
               :min     5
               :max     100
               :default 50}
   :stretch   {:type    :range
               :min     0.5
               :max     2
               :default 1}
   :mirrored? {:type    :boolean
               :default false}
   :reversed? {:type    :boolean
               :default false}})

(defn options [_charge]
  default-options)

(defn render [{:keys [type field tincture hints] :as charge} parent
              environment top-level-render render-options & {:keys [db-path]}]
  (if-let [charge-data-path (-> charge
                                get-charge-variant-data
                                :path)]
    (if-let [data @(rf/subscribe [:load-data charge-data-path])]
      (let [{:keys [position size stretch
                    mirrored? reversed?]}    (options/sanitize charge (options charge))
            ;; since size now is filled with a default, check whether it was set at all,
            ;; if not, then use nil
            ;; TODO: this probably needs a better mechanism and form representation
            size                             (if (:size charge) size nil)
            data                             (first data)
            points                           (:points environment)
            top                              (:top points)
            bottom                           (:bottom points)
            left                             (:left points)
            right                            (:right points)
            meta                             (get data 1)
            original-charge-width            (js/parseFloat (:width meta))
            original-charge-height           (js/parseFloat (:height meta))
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
                                                (min (/ target-width original-charge-width)
                                                     (/ target-height original-charge-height)))
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
                                                                 :billet} type)) squiggly-paths)
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
            charge-width                     (-> original-charge-width
                                                 (* scale-x)
                                                 Math/abs)
            charge-height                    (-> original-charge-height
                                                 (* scale-y)
                                                 Math/abs)
            position                         (-> (v/v charge-width charge-height)
                                                 (v/-)
                                                 (v// 2)
                                                 (v/+ center-point))
            shift                            (v/v (if mirrored? (- original-charge-width) 0)
                                                  (if reversed? (- original-charge-height) 0))
            charge-environment               (field-environment/create
                                              (svg/make-path ["M" position
                                                              "l" (v/v charge-width 0)
                                                              "l" (v/v 0 charge-height)
                                                              "l" (v/v (- charge-width) 0)
                                                              "l" (v/v 0 (- charge-height))
                                                              "z"])
                                              {:parent               field
                                               :context              [:charge]
                                               :bounding-box         (svg/bounding-box
                                                                      [position (v/+ position
                                                                                     (v/v charge-width charge-height))])
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
           [:rect {:x      (-> position :x (- 5))
                   :y      (-> position :y (- 5))
                   :width  (+ charge-width 10)
                   :height (+ charge-height 10)
                   :fill   "#fff"}]]]
         [:g {:clip-path (str "url(#" clip-path-id ")")}
          [:g {:transform (str "translate(" (:x position) "," (:y position) ") scale(" scale-x "," scale-y ") translate(" (-> shift :x) "," (-> shift :y) ")")
               :mask      (str "url(#" mask-inverted-id ")")}
           [:g {:transform (str "translate(" (-> shift :x -) "," (-> shift :y -) ") scale(" (/ 1 scale-x) "," (/ 1 scale-y) ") translate(" (- (:x position)) "," (- (:y position)) ")")}
            [top-level-render field charge-environment render-options :db-path (conj db-path :field)]]]
          [:g {:transform (str "translate(" (:x position) "," (:y position) ") scale(" scale-x "," scale-y ") translate(" (-> shift :x) "," (-> shift :y) ")")
               :mask      (str "url(#" mask-id ")")}
           coloured-charge]]])
      [:<>])
    [:<>]))
