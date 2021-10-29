(ns heraldry.frontend.ui.element.charge-group-preset-select
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.element.charge-group-preset-select-presets :as charge-group-preset-select-presets]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.static :as static]))

(macros/reg-event-db :select-charge-group-preset
  ;; TODO: this must not be an fn-traced, can be done once
  ;; https://github.com/day8/re-frame-debux/issues/40 is resolved
  (fn [db [_ path charge-group-preset charge-adjustments]]
    (let [new-db (-> db
                     (update-in path (fn [charge-group]
                                       (-> charge-group-preset
                                           (assoc :charges (:charges charge-group)))))
                     (assoc-in (conj path :charges 0 :anchor :point) :angle)
                     (assoc-in (conj path :charges 0 :anchor :angle) 0)
                     (assoc-in (conj path :charges 0 :geometry :size) nil))]
      (loop [new-db new-db
             [[rel-path value] & rest] charge-adjustments]
        (if (not rel-path)
          new-db
          (recur
           (assoc-in new-db (concat path rel-path) value)
           rest))))))

(defn charge-group-preset-choice [path key group charge-adjustments display-name]
  [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:select-charge-group-preset path group charge-adjustments])}
   [:img.clickable {:style {:width "4em"
                            :height "4.5em"}
                    :src (static/static-url
                          (str "/svg/charge-group-preset-" (name key) ".svg"))}]
   [:div.bottom
    [:h3 {:style {:text-align "center"}} [tr display-name]]
    [:i]]])

(defn charge-group-preset-select [path]
  [:div.ui-setting
   [:label [tr {:en "Presets"
                :de "Vorauswahl"}]]
   [:div.option
    [submenu/submenu path {:en "Select Charge Group Preset"
                           :de "Wappenfigurgruppen Vorauswahl"} {:en "Select"
                                                                 :de "Auswählen"} {:style {:width "21.5em"}}
     (for [[group-name & group] charge-group-preset-select-presets/presets]
       ^{:key group-name}
       [:<>
        [:h4 [tr group-name]]
        (for [[display-name key charge-group charge-adjustments] group]
          ^{:key display-name}
          [charge-group-preset-choice path key charge-group charge-adjustments display-name])])]]])
