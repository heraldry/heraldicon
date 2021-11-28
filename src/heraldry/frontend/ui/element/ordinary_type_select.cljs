(ns heraldry.frontend.ui.element.ordinary-type-select
  (:require
   [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.static :as static]
   [heraldry.util :as util]))

(defn -default-line-style-of-ordinary-type [ordinary-type]
  (case ordinary-type
    :heraldry.ordinary.type/gore :enarched
    :straight))

(macros/reg-event-db :set-ordinary-type
  (fn [db [_ path new-type]]
    (let [current (get-in db path)
          has-default-line-style? (-> current
                                      :line
                                      :type
                                      (= (-default-line-style-of-ordinary-type (:type current))))
          new-default-line-style (-default-line-style-of-ordinary-type new-type)
          new-flipped (case new-type
                        :heraldry.ordinary.type/gore true
                        false)]
      (-> db
          (assoc-in (conj path :type) new-type)
          (cond->
            has-default-line-style? (->
                                     (assoc-in (conj path :line :type) new-default-line-style)
                                     (assoc-in (conj path :line :flipped?) new-flipped)))
          ;; TODO: the switch to context here is problematic
          (update-in path #(util/deep-merge-with (fn [_current-value new-value]
                                                   new-value)
                                                 %
                                                 (options/sanitize-or-nil % (interface/options {:path %}))))))))

(defn ordinary-type-choice [path key display-name & {:keys [selected?
                                                            on-click?]
                                                     :or {on-click? true}}]
  [:div.choice.tooltip {:on-click (when on-click?
                                    #(state/dispatch-on-event % [:set-ordinary-type (vec (drop-last path)) key]))}
   [:img.clickable {:style {:width "5em"
                            :height "5.7em"}
                    :src (static/static-url
                          (str "/svg/ordinary-type-" (name key) "-" (if selected? "selected" "unselected") ".svg"))}]
   [:div.bottom
    [:h3 {:style {:text-align "center"}} [tr display-name]]
    [:i]]])

(defn ordinary-type-select [{:keys [path] :as context}]
  (when-let [option (interface/get-relevant-options context)]
    (let [current-value (interface/get-raw-data context)
          {:keys [ui inherited default choices]} option
          value (or current-value
                    inherited
                    default)
          choice-map (util/choices->map choices)
          choice-name (get choice-map value)
          label (:label ui)]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context {:en "Select Ordinary"
                                  :de "Heroldsbild auswählen"}
         [:div
          [:div
           [tr choice-name]
           [value-mode-select/value-mode-select context
            :display-fn ordinary-options/ordinary-map]]
          [ordinary-type-choice path value choice-name :on-click? false]]
         {:style {:width "21.5em"}}
         (for [[display-name key] choices]
           ^{:key key}
           [ordinary-type-choice path key display-name :selected? (= key value)])]]])))

(defmethod ui-interface/form-element :ordinary-type-select [context]
  [ordinary-type-select context])
