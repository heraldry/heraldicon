(ns heraldry.frontend.ui.element.field-type-select
  (:require [heraldry.coat-of-arms.field.core :as field]
            [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.options :as options]
            [heraldry.static :as static]
            [re-frame.core :as rf]))

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
                             default (field/default-fields prepared-field)
                             previous-default (field/default-fields (get-in db path))
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
                                  (options/sanitize-or-nil % (field-options/options %))))
          (update-in path dissoc :tincture)))))

(rf/reg-event-db :set-field-type
  (fn [db [_ path new-type num-fields-x num-fields-y num-base-fields]]
    (set-field-type db path new-type num-fields-x num-fields-y num-base-fields)))

(defn field-type-choice [path key display-name & {:keys [selected?]}]
  [:div.choice.tooltip {:on-click #(let [;; TODO: this should move into the event handler
                                         field-path (vec (drop-last path))
                                         field @(rf/subscribe [:get field-path])
                                         new-field (assoc field :type key)
                                         {:keys [num-fields-x
                                                 num-fields-y
                                                 num-base-fields]} (:layout (options/sanitize-or-nil
                                                                             new-field
                                                                             (field-options/options new-field)))]
                                     (state/dispatch-on-event % [:set-field-type field-path key num-fields-x num-fields-y num-base-fields]))}
   [:img.clickable {:style {:width "4em"
                            :height "4.5em"}
                    :src (static/static-url
                          (str "/svg/field-type-" (name key) "-" (if selected? "selected" "unselected") ".svg"))}]
   [:div.bottom
    [:h3 {:style {:text-align "center"}} display-name]
    [:i]]])

(defn field-type-select [path]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [current-value @(rf/subscribe [:get-value path])
          {:keys [ui inherited default choices]} option
          value (or current-value
                    inherited
                    default)
          label (:label ui)]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path "Select Division" (get field-options/field-map value) {:width "21.5em"}
         (for [[display-name key] choices]
           ^{:key key}
           [field-type-choice path key display-name :selected? (= key value)])]
        [value-mode-select/value-mode-select path
         :display-fn field-options/field-map]]])))

(defmethod interface/form-element :field-type-select [path]
  [field-type-select path])
