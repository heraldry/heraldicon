(ns heraldicon.frontend.ui.element.charge-group-preset-select
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.ui.element.charge-group-preset-select-presets :as charge-group-preset-select-presets]
   [heraldicon.frontend.ui.element.submenu :as submenu]
   [heraldicon.static :as static]))

(macros/reg-event-db :select-charge-group-preset
  ;; TODO: this must not be an fn-traced, can be done once
  ;; https://github.com/day8/re-frame-debux/issues/40 is resolved
  (fn [db [_ path charge-group-preset charge-adjustments]]
    (let [new-db (-> db
                     (update-in path (fn [charge-group]
                                       (assoc charge-group-preset :charges (:charges charge-group))))
                     (assoc-in (conj path :charges 0 :orientation :point) :angle)
                     (assoc-in (conj path :charges 0 :orientation :angle) 0)
                     (assoc-in (conj path :charges 0 :geometry :size) nil))]
      (loop [new-db new-db
             [[rel-path value] & rest] charge-adjustments]
        (if rel-path
          (recur
           (assoc-in new-db (concat path rel-path) value)
           rest)
          new-db)))))

(defn charge-group-preset-choice [path key group charge-adjustments display-name]
  [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:select-charge-group-preset path group charge-adjustments])}
   [:img.clickable {:style {:width "4em"
                            :height "4.5em"}
                    :src (static/static-url
                          (str "/svg/charge-group-preset-" (name key) ".svg"))}]
   [:div.bottom
    [:h3 {:style {:text-align "center"}} [tr display-name]]
    [:i]]])

(defn charge-group-preset-select [{:keys [path] :as context}]
  [:div.ui-setting
   [:label [tr :string.charge-group.presets/presets]]
   [:div.option
    [submenu/submenu context :string.charge-group/select-charge-group-preset
     [tr :string.charge-group.presets/select] {:style {:width "21.5em"}}
     (for [[group-name & group] charge-group-preset-select-presets/presets]
       ^{:key group-name}
       [:<>
        [:h4 [tr group-name]]
        (for [[display-name key charge-group charge-adjustments] group]
          ^{:key display-name}
          [charge-group-preset-choice path key charge-group charge-adjustments display-name])])]]])
