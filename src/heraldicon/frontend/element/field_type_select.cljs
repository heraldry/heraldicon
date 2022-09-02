(ns heraldicon.frontend.element.field-type-select
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.element.value-mode-select :as value-mode-select]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.tooltip :as tooltip]
   [heraldicon.heraldry.field.core :as field]
   [heraldicon.heraldry.field.options :as field.options]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]
   [heraldicon.static :as static]
   [re-frame.core :as rf]))

;; TODO: this needs some more thinking, currently it creates dummy contexts to access db data
(defn set-field-type [db path new-type num-fields-x num-fields-y num-base-fields]
  (let [path (vec path)]
    (if (= new-type :heraldry.field.type/plain)
      (-> db
          (assoc-in (conj path :type) new-type)
          (update-in (conj path :tincture) #(or % :none))
          (update-in path dissoc :fields))
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
                         (assoc prepared-field
                                :fields (->> (map vector merged previous-default default)
                                             (map (fn [[cur old-def def]]
                                                    (if (and (= (:type cur) :heraldry.subfield.type/field)
                                                             (not= cur old-def))
                                                      cur
                                                      def)))
                                             vec)))))
          (update-in path #(merge %
                                  (options/sanitize-or-nil % (interface/options {:path [:context :dummy]
                                                                                 :dummy %}))))
          (update-in path dissoc :tincture)))))

(macros/reg-event-db ::set
  (fn [db [_ path new-type]]
    (let [field-path (vec (drop-last path))
          field (get-in db field-path)
          new-field (assoc field :type key)
          {:keys [num-fields-x
                  num-fields-y
                  num-base-fields]} (:layout (options/sanitize-or-nil
                                              new-field
                                              (interface/options {:path [:context :dummy]
                                                                  :dummy new-field})))]
      (set-field-type db field-path new-type num-fields-x num-fields-y num-base-fields))))

(defn- field-type-choice [path key display-name & {:keys [selected?
                                                          clickable?]
                                                   :or {clickable? true}}]
  (let [choice [:img.clickable {:style {:width "4em"
                                        :height "4.5em"}
                                :on-click (when clickable?
                                            (js-event/handled #(rf/dispatch [::set path key])))
                                :src (static/static-url
                                      (str "/svg/field-type-" (name key) "-" (if selected? "selected" "unselected") ".svg"))}]]
    (if clickable?
      [tooltip/choice display-name choice]
      choice)))

(defmethod element/element :ui.element/field-type-select [{:keys [path] :as context}]
  (when-let [option (interface/get-relevant-options context)]
    (let [current-value (interface/get-raw-data context)
          {:keys [inherited default choices]
           :ui/keys [label]} option
          value (or current-value
                    inherited
                    default)
          choice-map (options/choices->map choices)
          choice-name (get choice-map value)]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context :string.option/select-partition
         [:div
          [:div
           [tr choice-name]]
          [value-mode-select/value-mode-select context
           :display-fn field.options/field-map]
          [:div {:style {:transform "translate(-0.3333em,0)"}}
           [field-type-choice path value choice-name :clickable? false]]]
         {:style {:width "21.5em"}}
         (into [:<>]
               (map (fn [[display-name key]]
                      ^{:key key}
                      [field-type-choice path key display-name :selected? (= key value)]))
               choices)]]])))
