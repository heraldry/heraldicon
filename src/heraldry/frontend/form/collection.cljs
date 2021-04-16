(ns heraldry.frontend.form.collection
  (:require [heraldry.frontend.form.element :as element]
            heraldry.frontend.form.state
            [re-frame.core :as rf]))

(defn form [path]
  (let [data @(rf/subscribe [:get path])]
    [element/component path :collection (:name data) "Collection"
     [:div.settings
      [element/range-input (conj path :num-columns) "Columns"
       1
       10
       :default 6]]

     [:div {:style {:margin-bottom "0.5em"}}
      [:button {:on-click nil #_ (state/dispatch-on-event % [:add-component path default/ordinary])}
       [:i.fas.fa-plus] " Add coat of arms"]]
     [:div.components
      [:ul]]]))

