(ns or.coad.charge
  (:require [clojure.string :as s]
            [clojure.walk :as walk]
            [or.coad.config :as config]
            [or.coad.tincture :as tincture]
            [or.coad.vector :as v]
            [re-frame.core :as rf]))

(def placeholder-regex
  (re-pattern (str "(?i)(" (s/join "|" (vals config/placeholder-colours)) ")")))

(defn find-charge [charge-map [group & rest]]
  (let [next (get-in charge-map [:groups group])]
    (if rest
      (recur next rest)
      next)))

(defn get-charge-map []
  @(rf/subscribe [:load-data "data/charge-map.edn"]))

(defn get-charge-variant-data [{:keys [type attitude variant]}]
  (when-let [charge-map (get-charge-map)]
    (let [lookup-path (get-in charge-map
                              [:lookup type])
          charge-data (get-in (find-charge charge-map lookup-path)
                              [:charges type])
          attitude-variants (get-in charge-data
                                    [:attitudes attitude :variants])
          variants (or attitude-variants
                       (:variants charge-data))]
      (get variants variant))))

(defn pick-placeholder-tincture [match {:keys [primary] :as tincture}]
  (let [lower-case-match (s/lower-case match)
        reverse-lookup (into {} (map (fn [[key value]]
                                       [(s/lower-case value) key])
                                     config/placeholder-colours))
        kind (get reverse-lookup lower-case-match)]
    (or (get tincture kind)
        primary)))

(defn replace-tinctures [string tincture]
  (s/replace string placeholder-regex (fn [[_ match]]
                                        (pick-placeholder-tincture match tincture))))

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

(defn replace-in-hiccup [hiccup tincture]
  (walk/postwalk #(cond-> %
                    (string? %) (replace-tinctures tincture)
                    (and (vector? %)
                         (-> % count (= 2))
                         (-> % first (= :style))) ((fn [[key value]]
                                                     (if (= key :style)
                                                       [:style (split-style-value value)]
                                                       [key value]))))
                 hiccup))

(defn remove-outlines [hiccup]
  (walk/postwalk #(cond-> %
                    (vector? %) ((fn [v]
                                   (if (or (= v [:stroke "#000"])
                                           (= v [:stroke "#0000000"]))
                                     [:stroke "none"]
                                     v))))
                 hiccup))

(defn render [{:keys [tincture hints] :as charge} environment options & {:keys [db-path]}]
  (if-let [charge-data-path (-> charge
                                get-charge-variant-data
                                :path)]
    (if-let [data @(rf/subscribe [:load-data charge-data-path])]
      (let [data (first data)
            meta (get data 1)
            width (js/parseFloat (:width meta))
            height (js/parseFloat (:height meta))
            target-width (* (:width environment) 0.8)
            target-height (* (:height environment) 0.7)
            scale (min (/ target-width width)
                       (/ target-height height))
            position (-> (v/v width height)
                         (v/* scale)
                         (v/-)
                         (v// 2)
                         (v/+ (-> environment :points :fess)))
            color-adjusted-data (replace-in-hiccup
                                 data
                                 (into {} (map (fn [[key value]]
                                                 [key (tincture/pick value options)])
                                               tincture)))
            adjusted-data (if (or (:outline? hints)
                                  (:outline? options))
                            color-adjusted-data
                            (remove-outlines color-adjusted-data))]
        [:g {:transform (str "translate(" (:x position) "," (:y position) ") scale(" scale "," scale ")")
             :on-click (fn [event]
                         (rf/dispatch [:select-component db-path])
                         (.stopPropagation event))
             :style {:pointer-events "visiblePainted"
                     :cursor "pointer"}}
         (assoc adjusted-data 0 :g)])
      [:<>])
    [:<>]))
