(ns heraldicon.frontend.element.metadata
  (:require
   [clojure.string :as s]
   [heraldicon.context :as c]
   [heraldicon.entity.metadata :as metadata]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.select :as select]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.util.sanitize :as sanitize]
   [re-frame.core :as rf]))

(def ^:private name-path
  [:ui :metadata :new-name])

(def ^:private value-path
  [:ui :metadata :new-value])

(defn- on-name-change [event]
  (let [new-value (-> event .-target .-value)]
    (rf/dispatch-sync [:set name-path new-value])))

(defn- on-value-change [event]
  (let [new-value (-> event .-target .-value)]
    (rf/dispatch-sync [:set value-path new-value])))

(defn- remove-metadata-name [metadata name]
  (into []
        (remove (fn [[n _]]
                  (= n name)))
        metadata))

(rf/reg-event-db ::clear-fields
  (fn [db _]
    (-> db
        (assoc-in name-path "")
        (assoc-in value-path ""))))

(macros/reg-event-db ::add-metadata
  (fn [db [_ context name value]]
    (let [name (sanitize/sanitize-string name)]
      (update-in db (:path context) (fn [metadata]
                                      (-> metadata
                                          (remove-metadata-name name)
                                          (conj [name value])))))))

(macros/reg-event-db ::remove-metadata
  (fn [db [_ context name]]
    (update-in db (:path context) remove-metadata-name name)))

(defmethod element/element :ui.element/metadata [context]
  (when-let [options (interface/get-options context)]
    (let [{:ui/keys [label]} options
          num-metadata (interface/get-list-size context)
          link-name (if (pos? num-metadata)
                      (string/str-tr :string.user.button/change " (" num-metadata ")")
                      :string.miscellaneous/none)]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label [tr link-name] {:style {:width "31em"}
                                                       :class "submenu-metadata"}
         (let [name-value (interface/get-raw-data (c/<< context :path name-path))
               value-value (interface/get-raw-data (c/<< context :path value-path))
               metadata (interface/get-raw-data context)]
           [:<>
            [:div.ui-setting {:style {:margin-top "10px"
                                      :white-space "nowrap"}}
             [:div.option
              [select/raw-select-inline
               nil
               :custom
               (into [[:string.option/select-preset :preset]]
                     (map (fn [k]
                            [k k]))
                     (sort metadata/known-metadata-keys))
               :keywordize? false
               :on-change (fn [value]
                            (when (not= value :preset)
                              (rf/dispatch [:set name-path value])))]
              [:br]
              [:input {:value name-value
                       :on-change on-name-change
                       :type "text"
                       :style {:margin-right "0.5em"}}]
              [:input {:value value-value
                       :on-change on-value-change
                       :type "text"
                       :style {:margin-right "0.5em"}}]
              [:button
               {:disabled (or (-> name-value (or "") s/trim count zero?)
                              (-> value-value (or "") s/trim count zero?))
                :on-click (js-event/handled
                           #(do
                              (rf/dispatch [::add-metadata context name-value value-value])
                              (rf/dispatch [::clear-fields])))
                :type "button"}
               [tr :string.button/add]]]]
            [:hr]
            (into [:<>]
                  (map (fn [[n v]]
                         ^{:key n}
                         [:div.ui-setting {:style {:margin-top "10px"
                                                   :white-space "nowrap"}}
                          [:label n]
                          [:div.option
                           [:input {:value v
                                    :on-change (js-event/stop-propagation
                                                #(rf/dispatch-sync [::add-metadata context n (-> % .-target .-value)]))
                                    :type "text"
                                    :style {:margin-right "0.5em"}}]
                           [:button
                            {:on-click #(rf/dispatch [::remove-metadata context n])
                             :type "button"}
                            [tr :string.option.outline-mode-choice/remove]]]]))
                  (sort metadata))])]]])))
