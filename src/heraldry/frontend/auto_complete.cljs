(ns heraldry.frontend.auto-complete
  (:require
   [heraldry.frontend.macros :as macros]
   [re-frame.core :as rf]))

(def db-path
  [:ui :auto-complete])

(macros/reg-event-db :set-auto-complete-data
  (fn [db [_ value]]
    (update-in db db-path merge value)))

(defn set-data [data]
  (rf/dispatch [:set-auto-complete-data data]))

(defn clear-data []
  (rf/dispatch [:set db-path nil]))

(rf/dispatch [:remove db-path])

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
            (map (fn [[choice hint]]
                   [:li.auto-complete-suggestion {:on-click (fn [event]
                                                              (doto event
                                                                .preventDefault
                                                                .stopPropagation)
                                                              (when on-click
                                                                (on-click choice)))}
                    choice
                    (when hint
                      [:span.hint hint])]))
            choices))))

(defn auto-complete-first []
  (let [{:keys [choices
                position
                on-click]} @(rf/subscribe [:get db-path])]
    (when (and position
               (seq choices))
      (on-click (ffirst choices)))))
