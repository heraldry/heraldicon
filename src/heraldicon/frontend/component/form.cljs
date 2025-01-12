(ns heraldicon.frontend.component.form
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.language :refer [tr]]
   [re-frame.core :as rf]))

(defn- form [context]
  (let [{:keys [title]} (tree/node-data context)
        form (component/form context)
        {:keys [form effective-context]} (if (map? form)
                                           form
                                           {:form form
                                            :effective-context context})]
    [:div.ui-component
     [:div.ui-component-header
      [:h1
       [:i.fa.fa-sliders-h.ui-icon {:style {:margin-right "0.5em"}}]
       [tr title]]]
     [:div.content
      (when form
        [form effective-context])]]))

(defn active [{::tree/keys [identifier]
               :as context}]
  (let [selected-component-path @(rf/subscribe [::tree/active-node-path identifier])]
    (when selected-component-path
      [form (c/<< context :path selected-component-path)])))
