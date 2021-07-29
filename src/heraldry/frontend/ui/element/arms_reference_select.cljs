(ns heraldry.frontend.ui.element.arms-reference-select
  (:require [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.arms-select :as arms-select]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [re-frame.core :as rf]))

(defn link-to-arms [path]
  (fn [arms]
    [:a {:on-click #(rf/dispatch [:set path {:id (:id arms)
                                             :version 0}])}
     (:name arms)]))

(defn arms-reference-select [path]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [{arms-id :id
           version :version} @(rf/subscribe [:get-value path])
          {:keys [ui]} option
          label (:label ui)
          [_status arms-data] (when arms-id
                                (state/async-fetch-data
                                 [:arms-references arms-id version]
                                 [arms-id version]
                                 #(arms-select/fetch-arms arms-id version nil)))
          arms-title (-> arms-data
                         :name
                         (or "None"))]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path "Select Arms" arms-title nil
         [arms-select/list-arms (link-to-arms path)]]]])))

(defmethod interface/form-element :arms-reference-select [path]
  [arms-reference-select path])
