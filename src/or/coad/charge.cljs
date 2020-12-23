(ns or.coad.charge
  (:require [clojure.string :as s]
            [clojure.walk :as walk]
            [or.coad.tincture :as tincture]
            [or.coad.vector :as v]
            [re-frame.core :as rf]))

(def placeholder-colours
  {:primary "#214263"
   :armed "#848401"
   :langued "#840101"
   :attired "#010184"
   :unguled "#018401"
   :eyes-and-teeth "#848484"})

(def placeholder-regex
  (re-pattern (str "(?i)(" (s/join "|" (vals placeholder-colours)) ")")))

(defn find-charge [charge-map [group & rest]]
  (let [next (-> charge-map :groups group)]
    (if rest
      (recur next rest)
      next)))

(defn get-charge-data-path [{:keys [type attitude variant]}]
  (if-let [charge-map @(rf/subscribe [:load-data "data/charge-map.edn"])]
    (let [lookup-path (-> charge-map :lookup type)
          charge-data (-> (find-charge charge-map lookup-path)
                          :charges type)
          attitude-variants (-> charge-data :attitudes (get attitude) :variants)
          variants (or attitude-variants
                       (-> charge-data :variants))
          variant-data (get variants variant)]
      (:path variant-data))))

(defn pick-placeholder-tincture [match {:keys [primary] :as tincture}]
  (let [lowercase-match (s/lower-case match)
        reverse-lookup (into {} (map (fn [[key value]] [(s/lower-case value) key]) placeholder-colours))
        kind (get reverse-lookup lowercase-match)]
    (or (get tincture kind)
        primary)))

(defn replace-tinctures [string tincture]
  (s/replace string placeholder-regex (fn [[_ match]]
                                        (pick-placeholder-tincture match tincture))))

(defn replace-in-hiccup [hiccup tincture]
  (walk/postwalk #(cond-> %
                    (string? %) (replace-tinctures tincture))
                 hiccup))

(defn remove-outlines [hiccup]
  (walk/postwalk #(cond-> %
                    (vector? %) ((fn [v]
                                   (if (or (= v [:stroke "#000"])
                                           (= v [:stroke "#0000000"]))
                                     [:stroke "none"]
                                     v))))
                 hiccup))

(defn render [{:keys [tincture hints] :as charge} environment options]
  (if-let [charge-data-path (get-charge-data-path charge)]
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
        [:g {:transform (str "translate(" (:x position) "," (:y position) ") scale(" scale "," scale ")")}
         (assoc adjusted-data 0 :g)])
      [:<>])
    [:<>]))
