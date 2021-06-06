(ns heraldry.frontend.form.arms-reference
  (:require [heraldry.frontend.form.arms-select :as arms-select]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.user :as user]
            [re-frame.core :as rf]))

(defn link-to-arms [path]
  (fn [arms]
    [:a {:on-click #(rf/dispatch [:set path (select-keys arms [:id :version])])}
     (:name arms)]))

(defn form [path]
  (let [{arms-id :id
         version :version} @(rf/subscribe [:get path])
        [_status arms-data] (when arms-id
                              (state/async-fetch-data
                               [:arms-references arms-id version]
                               [arms-id version]
                               #(arms-select/fetch-arms arms-id version nil)))
        arms-title (-> arms-data
                       :name
                       (or "None"))]
    [:div.setting {:style {:margin-bottom "0.5em"}}
     [:label "Arms"]
     " "
     [element/submenu path "Select Arms" arms-title {}
      [arms-select/list-arms (link-to-arms path)]]]))
