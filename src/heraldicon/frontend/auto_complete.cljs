(ns heraldicon.frontend.auto-complete
  (:require
   [clojure.string :as s]
   [re-frame.core :as rf]))

(def ^:private db-path
  [:ui :auto-complete])

(rf/reg-event-db ::set
  (fn [db [_ value]]
    (update-in db db-path merge value)))

(rf/reg-event-db ::clear
  (fn [db [_]]
    (assoc-in db db-path nil)))

(rf/reg-event-db ::apply-first
  (fn [db _]
    (let [{:keys [choices
                  position
                  on-click]} (get-in db db-path)]
      (when (and position
                 (seq choices))
        (on-click (ffirst choices)))
      db)))

(defn render []
  (let [{:keys [choices
                position
                on-click]} @(rf/subscribe [:get db-path])]
    (when (and position
               (seq choices))
      (into [:ul.auto-complete-box {:style {:left (-> position
                                                      :left
                                                      (str "px"))
                                            :top (str "calc(" (:top position) "px + 5px + 1em)")}}]
            (keep (fn [[choice hint]]
                    (when-not (s/blank? choice)
                      [:li.auto-complete-suggestion {:on-click (fn [event]
                                                                 (doto event
                                                                   .preventDefault
                                                                   .stopPropagation)
                                                                 (when on-click
                                                                   (on-click choice)))}
                       choice
                       (when hint
                         [:span.hint hint])])))
            choices))))
