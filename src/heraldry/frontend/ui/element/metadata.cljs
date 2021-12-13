(ns heraldry.frontend.ui.element.metadata
  (:require
   [clojure.string :as s]
   [heraldry.context :as c]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.ui.element.select :as select]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.metadata :as metadata]
   [re-frame.core :as rf]
   [heraldry.util :as util]))

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

(rf/reg-event-db ::add-metadata
  (fn [db [_ context name value]]
    (let [name (util/sanitize-string name)]
      (update-in db (:path context) (fn [metadata]
                                      (-> metadata
                                          (remove-metadata-name name)
                                          (conj [name value])))))))

(defn on-add [context]
  (let [name (interface/get-raw-data {:path name-path})
        value (interface/get-raw-data {:path value-path})]
    (rf/dispatch [::add-metadata context name value])))

(rf/reg-event-db ::remove-metadata
  (fn [db [_ context name]]
    (update-in db (:path context) remove-metadata-name name)))

(defn metadata-submenu [context]
  (when-let [options (interface/get-relevant-options context)]
    (let [{:keys [ui]} options
          label (:label ui)
          num-metadata (interface/get-list-size context)
          link-name (if (pos? num-metadata)
                      (util/str-tr (string "Change") " (" num-metadata ")")
                      (string "None"))]
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
               (into [[(string "Select Preset") :preset]]
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
               [tr (string "Add")]]]]
            [:hr]
            (doall
             (for [[n v] (sort metadata)]
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
                  [tr (string "Remove")]]]])
             )])]]])))

(defmethod ui-interface/form-element :metadata [context]
  [metadata-submenu context])
