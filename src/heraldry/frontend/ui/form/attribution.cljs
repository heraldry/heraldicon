(ns heraldry.frontend.ui.form.attribution
  (:require [heraldry.frontend.ui.interface :as interface]))

(defn form [path _]
  [:<>
   (for [option [:nature
                 :license
                 :license-version]]
     ^{:key option} [interface/form-element (conj path option)])

   [:div {:style {:height "1.5em"}}]

   (for [option [:source-license
                 :source-license-version
                 :source-name
                 :source-link
                 :source-creator-name
                 :source-creator-link]]
     ^{:key option} [interface/form-element (conj path option)])])

(defmethod interface/component-node-data :heraldry.component/attribution [_path _component-data _component-options]
  {:title "Attribution"})

(defmethod interface/component-form-data :heraldry.component/attribution [_path _component-data _component-options]
  {:form form})
