(ns heraldicon.frontend.ui.element.metadata
  (:require
   [clojure.string :as s]
   [heraldicon.context :as c]
   [heraldicon.entity.metadata :as metadata]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.ui.element.select :as select]
   [heraldicon.frontend.ui.element.submenu :as submenu]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.util.sanitize :as sanitize]
   [re-frame.core :as rf]))

(def name-path [:ui :metadata :new-name])
(def value-path [:ui :metadata :new-value])

(defn on-name-change [event]
  (let [new-value (-> event .-target .-value)]
    (rf/dispatch-sync [:set name-path new-value])))

(defn on-value-change [event]
  (let [new-value (-> event .-target .-value)]
    (rf/dispatch-sync [:set value-path new-value])))

(defn remove-metadata-name [metadata name]
  (into []
        (remove (fn [[n _]]
                  (= n name)))
        metadata))

(macros/reg-event-db ::add-metadata
  (fn [db [_ context name value]]
    (let [name (sanitize/sanitize-string name)]
      (update-in db (:path context) (fn [metadata]
                                      (-> metadata
                                          (remove-metadata-name name)
                                          (conj [name value])))))))

(defn on-add [context]
  (let [name (interface/get-raw-data {:path name-path})
        value (interface/get-raw-data {:path value-path})]
    (rf/dispatch-sync [::add-metadata context name value])
    (rf/dispatch-sync [:set name-path ""])
    (rf/dispatch-sync [:set value-path ""])))

(macros/reg-event-db ::remove-metadata
  (fn [db [_ context name]]
    (update-in db (:path context) remove-metadata-name name)))

(defn metadata-submenu [context]
  (when-let [options (interface/get-relevant-options context)]
    (let [{:keys [ui]} options
          label (:label ui)
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
                :on-click (partial on-add context)
                :type "button"}
               [tr :string.button/add]]]]
            [:hr]
            (into [:<>]
                  (map (fn [[n v]]
                         ^{:key n}
                         [:div.ui-setting {:style {:margin-top "10px"
                                                   :white-space "nowrap"}}
                          [:label
                           n]
                          [:div.option
                           [:input {:value v
                                    :on-change #(rf/dispatch-sync [::add-metadata context
                                                                   n
                                                                   (-> % .-target .-value)])
                                    :type "text"
                                    :style {:margin-right "0.5em"}}]
                           [:button
                            {:on-click #(rf/dispatch [::remove-metadata context n])
                             :type "button"}
                            [tr :string.option.outline-mode-choice/remove]]]]))
                  (sort metadata))])]]])))

(defmethod ui.interface/form-element :metadata [context]
  [metadata-submenu context])
