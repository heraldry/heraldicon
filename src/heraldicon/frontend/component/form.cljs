(ns heraldicon.frontend.component.form
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.language :refer [tr]]
   [re-frame.core :as rf]))

(defn- form [context]
  (let [{:keys [title]} (tree/node-data context)
        form (component/form context)]
    [:div.ui-component
     [:div.ui-component-header
      [:h1
       [:i.fa.fa-sliders-h.ui-icon {:style {:margin-right "0.5em"}}]
       [tr title]]]
     [:div.content
      (when form
        [form context])]]))

(defn active []
  (let [selected-component-path @(rf/subscribe [::tree/active-node-path])]
    [form (when selected-component-path
            (c/<< tree/node-context :path selected-component-path))]))
