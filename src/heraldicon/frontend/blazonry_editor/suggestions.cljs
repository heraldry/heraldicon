(ns heraldicon.frontend.blazonry-editor.suggestions
  (:require
   [clojure.string :as s]
   [heraldicon.frontend.auto-complete :as auto-complete]
   [re-frame.core :as rf]))

(def ^:private hint-order
  (into {}
        (map-indexed (fn [index hint]
                       [hint index]))
        ["layout"
         "cottising"
         "line"
         "fimbriation"
         "extra tincture"
         "tincture"
         "ordinary"
         "ordinary option"
         "charge"
         "charge option"
         "partition"
         "attitude"
         "facing"
         "number"]))

(defn- filter-choices [choices s]
  (->> choices
       (filter (fn [[choice _]]
                 (s/starts-with? choice s)))
       (sort-by (fn [[choice hint]]
                  [(get hint-order hint 1000) choice]))))

(rf/reg-event-fx ::set
  (fn [_ [_ caret-position suggestions substring-since-error on-click]]
    (let [auto-complete (when suggestions
                          {:choices (filter-choices suggestions substring-since-error)
                           :on-click on-click
                           :position caret-position})]
      {:dispatch (if auto-complete
                   [::auto-complete/set auto-complete]
                   [::auto-complete/clear])})))
