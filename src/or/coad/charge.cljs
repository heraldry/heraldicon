(ns or.coad.charge
  (:require [clojure.string :as s]
            [clojure.walk :as walk]
            [or.coad.tincture :as tincture]
            [or.coad.vector :as v]
            [re-frame.core :as rf]))

(def placeholder-primary "#214263")
(def placeholder-armed "#424201")
(def placeholder-langued "#420001")
(def placeholder-attired "#010142")
(def placeholder-unguled "#014201")

(def placeholder-regex
  (re-pattern (str "(?i)(" (s/join "|" [placeholder-primary
                                        placeholder-armed
                                        placeholder-langued
                                        placeholder-attired
                                        placeholder-unguled]) ")")))

(defn get-charge-name [type attitude]
  "charges/animal/wolf/passant/wolf-1.edn")

(defn replace-tinctures [string tincture]
  (s/replace string placeholder-regex (fn [[_ match]]
                                        (let [primary         (:primary tincture)
                                              lowercase-match (s/lower-case match)]
                                          (cond
                                            (= lowercase-match
                                               placeholder-primary) primary
                                            (= lowercase-match
                                               placeholder-armed)   (or (:armed tincture) primary)
                                            (= lowercase-match
                                               placeholder-langued) (or (:langued tincture) primary)
                                            (= lowercase-match
                                               placeholder-attired) (or (:attired tincture) primary)
                                            (= lowercase-match
                                               placeholder-unguled) (or (:unguled tincture) primary))))))

(defn replace-in-hiccup [hiccup tincture]
  (walk/postwalk #(cond-> %
                    (string? %) (replace-tinctures tincture))
                 hiccup))

(defn render [{:keys [type attitude tincture]} environment top-level-render options]
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
                                 (into {} (map (fn [[key value]]
                                                 [key (tincture/pick value options)])
                                               tincture)))]
        [:g {:transform (str "translate(" (:x position) "," (:y position) ") scale(" scale "," scale ")")}
         (assoc color-adjusted-data 0 :g)])
      [:<>])))
