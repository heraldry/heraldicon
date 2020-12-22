(ns or.coad.charge
  (:require [clojure.string :as s]
            [clojure.walk :as walk]
            [or.coad.tincture :as tincture]
            [or.coad.vector :as v]
            [re-frame.core :as rf]))

(defn get-charge-name [type attitude]
  "animal/wolf/rampant/wolf-1.edn")

(defn replace-tinctures [string primary secondary]
  (s/replace string #"(#ccc|#e60000)" (fn [[_ match]]
                                        (case match
                                          "#ccc"    primary
                                          "#e60000" secondary))))

(defn replace-in-hiccup [hiccup primary secondary]
  (walk/postwalk #(cond-> %
                    (string? %) (replace-tinctures primary secondary))
                 hiccup))

(defn render [{:keys [type attitude primary secondary]} environment top-level-render options]
  (let [charge-name (get-charge-name type attitude)]
    (if-let [data @(rf/subscribe [:charge-data charge-name])]
      (let [meta                (get data 1)
            width               (js/parseFloat (:width meta))
            height              (js/parseFloat (:height meta))
            target-width        (* (:width environment) 0.8)
            scale               (/ target-width width)
            position            (-> (v/v width height)
                                    (v/* scale)
                                    (v/-)
                                    (v// 2)
                                    (v/+ (-> environment :points :fess)))
            color-adjusted-data (replace-in-hiccup
                                 data
                                 (tincture/pick primary options)
                                 (tincture/pick secondary options))]
        [:g {:transform (str "translate(" (:x position) "," (:y position) ") scale(" scale "," scale ")")}
         (assoc color-adjusted-data 0 :g)])
      [:<>])))
