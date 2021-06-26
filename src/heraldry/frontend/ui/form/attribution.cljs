(ns heraldry.frontend.ui.form.attribution
  (:require [heraldry.frontend.ui.interface :as interface]
            [heraldry.license :as attribution]))

(defn form [path {:keys [options]}]
  [:<>
   (for [option [:nature
                 :license
                 :license-version]]
     ^{:key option} [interface/form-element (conj path option) (get options option)])

   [:div {:style {:height "1.5em"}}]

   (for [option [:source-license
                 :source-license-version
                 :source-name
                 :source-link
                 :source-creator-name
                 :source-creator-link]]
     ^{:key option} [interface/form-element (conj path option) (get options option)])])

(defmethod interface/component-node-data :heraldry.component/attribution [_path _component-data]
  {:title "Attribution"})

(defmethod interface/component-form-data :heraldry.component/attribution [component-data]
  {:form form
   :form-args {:options (attribution/options component-data)}})
