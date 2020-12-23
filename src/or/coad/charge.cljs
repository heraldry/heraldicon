(ns or.coad.charge
  (:require [clojure.string :as s]
            [clojure.walk :as walk]
            [or.coad.tincture :as tincture]
            [or.coad.vector :as v]
            [re-frame.core :as rf]))

(def placeholder-primary "#214263")
(def placeholder-armed "#848401")
(def placeholder-langued "#840101")
(def placeholder-attired "#010184")
(def placeholder-unguled "#018401")
(def placeholder-eyes-and-teeth "#848484")

(def placeholder-regex
  (re-pattern (str "(?i)(" (s/join "|" [placeholder-primary
                                        placeholder-armed
                                        placeholder-langued
                                        placeholder-attired
                                        placeholder-unguled
                                        placeholder-eyes-and-teeth]) ")")))

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
          attitude-variants (-> charge-data :attitudes attitude :variants)
          variants (or attitude-variants
                       (-> charge-data :variants))
          variant-data (get variants variant)]
      (:path variant-data))))

(defn replace-tinctures [string tincture]
  (s/replace string placeholder-regex (fn [[_ match]]
                                        (let [primary (:primary tincture)
                                              lowercase-match (s/lower-case match)]
                                          (cond
                                            (= lowercase-match
                                               placeholder-primary) primary
                                            (= lowercase-match
                                               placeholder-armed) (or (:armed tincture) primary)
                                            (= lowercase-match
                                               placeholder-langued) (or (:langued tincture) primary)
                                            (= lowercase-match
                                               placeholder-attired) (or (:attired tincture) primary)
                                            (= lowercase-match
                                               placeholder-unguled) (or (:unguled tincture) primary)
                                            (= lowercase-match
                                               placeholder-eyes-and-teeth) (or (:eyes-and-teeth tincture) primary)
                                            :else match)))))

(defn replace-in-hiccup [hiccup tincture]
  (walk/postwalk #(cond-> %
                    (string? %) (replace-tinctures tincture))
                 hiccup))

(defn render [{:keys [tincture] :as charge} environment options]
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
                                               tincture)))]
        [:g {:transform (str "translate(" (:x position) "," (:y position) ") scale(" scale "," scale ")")}
         (assoc color-adjusted-data 0 :g)])
      [:<>])
    [:<>]))
