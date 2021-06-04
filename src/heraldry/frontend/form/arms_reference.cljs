(ns heraldry.frontend.form.arms-reference
  (:require [heraldry.frontend.form.arms-select :as arms-select]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.user :as user]
            [re-frame.core :as rf]))

(defn link-to-arms [db-path]
  (fn [arms]
    [:a {:on-click #(rf/dispatch [:set db-path (select-keys arms [:id :version])])}
     (:name arms)]))

(defn form [db-path]
  (let [index (last db-path)
        data @(rf/subscribe [:get db-path])
        title (-> data
                  :name
                  (or "None"))
        {arms-id :id
         version :version} (:reference data)
        [_status arms-data] (when arms-id
                              (state/async-fetch-data
                               [:arms-references arms-id version]
                               [arms-id version]
                               #(arms-select/fetch-arms arms-id version nil)))
        arms-title (-> arms-data
                       :name
                       (or "None"))]
    [element/component db-path :arms-reference (str (inc index) ": " title) nil
     [:div.setting {:style {:margin-bottom "1em"}}
      [element/text-field (conj db-path :name) "Name" :style {:width "19em"}]]

     [:div.setting {:style {:margin-bottom "0.5em"}}
      [:label "Arms"]
      " "
      [element/submenu (conj db-path :reference) "Select Arms" arms-title {}
       [arms-select/list-arms-for-user (:user-id (user/data)) (link-to-arms (conj db-path :reference))]]]]))
