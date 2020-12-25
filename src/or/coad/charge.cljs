(ns or.coad.charge
  (:require [clojure.string :as s]
            [clojure.walk :as walk]
            [or.coad.config :as config]
            [or.coad.line :as line]
            [or.coad.svg :as svg]
            [or.coad.tincture :as tincture]
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
  (s/replace string placeholder-regex (fn [[_ match]]
                                        (pick-placeholder-tincture match tincture))))

(defn replace-non-placeholder-colour [string colour]
  (s/replace string placeholder-regex (fn [[_ match]]
                                        (println "match" match)
                                        (let [match (s/lower-case match)]
                                          (if (get config/placeholder-colours-set match)
                                            match
                                            colour)))))

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

(defn replace-non-placeholder-colours-everywhere [data colour]
  (walk/postwalk #(if (and (vector? %)
                           (-> % second string?)
                           (->> % first (get #{:stroke :fill})))
                    [(first %) (replace-non-placeholder-colour (second %) colour)]
                    %)
                 data))

(defn remove-outlines [data]
  (walk/postwalk #(cond-> %
                    (vector? %) ((fn [v]
                                   (if (or (= v [:stroke "#000"])
                                           (= v [:stroke "#0000000"]))
                                     [:stroke "none"]
                                     v))))
                 data))

(defn squiggly-paths [data]
  (walk/postwalk #(cond-> %
                    (vector? %) ((fn [v]
                                   (if (= (first v) :d)
                                     [:d (line/squiggly-path (second v))]
                                     v))))
                 data))

(defn make-mask [data]
  (let [mask-id       (svg/id "mask")
        adjusted-data (-> data
                          (replace-non-placeholder-colours-everywhere "#fff")
                          #_(replace-placeholder-colours-everywhere {:primary "#000"}))]
    [mask-id adjusted-data]))

(defn render [{:keys [type tincture hints ui] :as charge} environment options & {:keys [db-path]}]
  (if-let [charge-data-path (-> charge
                                get-charge-variant-data
                                :path)]
    (if-let [data @(rf/subscribe [:load-data charge-data-path])]
      (let [data            (first data)
            meta            (get data 1)
            width           (js/parseFloat (:width meta))
            height          (js/parseFloat (:height meta))
            target-width    (* (:width environment) 0.8)
            target-height   (* (:height environment) 0.7)
            scale           (min (/ target-width width)
                                 (/ target-height height))
            position        (-> (v/v width height)
                                (v/* scale)
                                (v/-)
                                (v// 2)
                                (v/+ (-> environment :points :fess)))
            adjusted-charge (-> data
                                fix-string-style-values
                                (cond->
                                    (not (or (:outline? hints)
                                             (:outline? options))) remove-outlines
                                    (and (:squiggly? options)
                                         (get #{:roundel
                                                :fusil
                                                :billet} type)) squiggly-paths)
                                (assoc 0 :g))
            [mask-id mask]  (make-mask adjusted-charge)
            coloured-charge (replace-placeholder-colours-everywhere
                             adjusted-charge
                             (-> {}
                                 (into (map (fn [[key value]]
                                              [key (if (= key :primary)
                                                     "none"
                                                     (tincture/pick value options))])
                                            tincture))))]
        [:<>
         [:mask {:id mask-id}
          mask]
         [:g {:transform (str "translate(" (:x position) "," (:y position) ") scale(" scale "," scale ")")
              ;; :mask      (str "url(#" mask-id ")")
              :on-click  (fn [event]
                           (rf/dispatch [:select-component db-path])
                           (.stopPropagation event))
              :style     {:pointer-events "visiblePainted"
                          :cursor         "pointer"
                          :filter         (when (-> ui :selected?) "url(#glow)")}}
          ;; coloured-charge
          mask]])
      [:<>])
    [:<>]))
