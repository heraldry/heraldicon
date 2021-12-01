(ns heraldry.frontend.ui.element.field-type-select
  (:require
   [heraldry.coat-of-arms.field.core :as field]
   [heraldry.coat-of-arms.field.options :as field-options]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.static :as static]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

;; TODO: this needs some more thinking, currently it creates dummy contexts to access db data
(defn set-field-type [db path new-type num-fields-x num-fields-y num-base-fields]
  (let [path (vec path)]
    (if (= new-type :heraldry.field.type/plain)
      (-> db
          (assoc-in (conj path :type) new-type)
          (update-in (conj path :tincture) #(or % :none)))
      (-> db
          (assoc-in (conj path :type) new-type)
          (update-in (conj path :line :type) #(or % :straight))
          (assoc-in (conj path :layout :num-fields-x) num-fields-x)
          (assoc-in (conj path :layout :num-fields-y) num-fields-y)
          (assoc-in (conj path :layout :num-base-fields) num-base-fields)
          (update-in path
                     (fn [prepared-field]
                       (let [current (or (:fields prepared-field) [])
                             default (field/default-fields {:path [:context :dummy]
                                                            :dummy prepared-field})
                             previous-default (field/default-fields {:path [:context :dummy]
                                                                     :dummy (get-in db path)})
                             previous-default (cond
                                                (< (count previous-default) (count default)) (into previous-default (subvec default (count previous-default)))
                                                (> (count previous-default) (count default)) (subvec previous-default 0 (count default))
                                                :else previous-default)
                             merged (cond
                                      (< (count current) (count default)) (into current (subvec default (count current)))
                                      (> (count current) (count default)) (subvec current 0 (count default))
                                      :else current)]
                         (-> prepared-field
                             (assoc :fields (->> (map vector merged previous-default default)
                                                 (map (fn [[cur old-def def]]
                                                        (if (and (-> cur
                                                                     :type
                                                                     (not= :heraldry.field.type/ref))
                                                                 (not= cur old-def))
                                                          cur
                                                          def)))
                                                 vec))))))
          (update-in path #(merge %
                                  (options/sanitize-or-nil % (interface/options {:path [:context :dummy]
                                                                                 :dummy %}))))
          (update-in path dissoc :tincture)))))

(macros/reg-event-db :set-field-type
  (fn [db [_ path new-type num-fields-x num-fields-y num-base-fields]]
    (set-field-type db path new-type num-fields-x num-fields-y num-base-fields)))

(defn field-type-choice [path key display-name & {:keys [selected?
                                                         on-click?]
                                                  :or {on-click? true}}]
  [:div.choice.tooltip {:on-click (when on-click?
                                    #(let [;; TODO: this should move into the event handler
                                           field-path (vec (drop-last path))
                                           field @(rf/subscribe [:get field-path])
                                           new-field (assoc field :type key)
                                           {:keys [num-fields-x
                                                   num-fields-y
                                                   num-base-fields]} (:layout (options/sanitize-or-nil
                                                                               new-field
                                                                               (interface/options {:path [:context :dummy]
                                                                                                   :dummy new-field})))]
                                       (state/dispatch-on-event % [:set-field-type field-path key num-fields-x num-fields-y num-base-fields])))}
   [:img.clickable {:style {:width "4em"
                            :height "4.5em"}
                    :src (static/static-url
                          (str "/svg/field-type-" (name key) "-" (if selected? "selected" "unselected") ".svg"))}]
   [:div.bottom
    [:h3 {:style {:text-align "center"}} [tr display-name]]
    [:i]]])

(defn field-type-select [{:keys [path] :as context}]
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
        [submenu/submenu context (string "Select Partition")
         [:div
          [:div
           [tr choice-name]]
          [value-mode-select/value-mode-select context
           :display-fn field-options/field-map]
          [:div {:style {:transform "translate(-0.3333em,0)"}}
           [field-type-choice path value choice-name :on-click? false]]]
         {:style {:width "21.5em"}}
         (for [[display-name key] choices]
           ^{:key key}
           [field-type-choice path key display-name :selected? (= key value)])]]])))

(defmethod ui-interface/form-element :field-type-select [context]
  [field-type-select context])
