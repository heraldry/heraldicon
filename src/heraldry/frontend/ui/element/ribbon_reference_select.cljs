(ns heraldry.frontend.ui.element.ribbon-reference-select
  (:require [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.ribbon-select :as ribbon-select]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [re-frame.core :as rf]))

(defn link-to-ribbon [path]
  (fn [ribbon]
    [:a {:on-click #(rf/dispatch [:set path {:id (:id ribbon)
                                             :version 0}])}
     (:name ribbon)]))

(defn ribbon-reference-select [path]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [{ribbon-id :id
           version :version} @(rf/subscribe [:get-value path])
          {:keys [ui]} option
          label (:label ui)
          [_status ribbon-data] (when ribbon-id
                                  (state/async-fetch-data
                                   [:ribbon-references ribbon-id version]
                                   [ribbon-id version]
                                   #(ribbon-select/fetch-ribbon ribbon-id version nil)))
          ribbon-title (-> ribbon-data
                           :name
                           (or "None"))]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path "Select Ribbon" ribbon-title nil
         [ribbon-select/list-ribbon (link-to-ribbon path)]]]])))

(defmethod interface/form-element :ribbon-reference-select [path]
  [ribbon-reference-select path])

