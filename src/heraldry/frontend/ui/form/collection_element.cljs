(ns heraldry.frontend.ui.form.collection-element
  (:require [heraldry.frontend.ui.interface :as interface]
            [re-frame.core :as rf]))

(def ui-highlighted-element-path [:ui :collection-library :selected-element])

(rf/reg-sub :collection-library-highlighted-element
  (fn [_ _]
    (rf/subscribe [:get ui-highlighted-element-path]))

  (fn [value [_ _path]]
    value))

(defn highlight-element [path]
  (rf/dispatch-sync [:set ui-highlighted-element-path path]))

(defn form [path _]
  [:<>
   (for [option [:name
                 :reference]]
     ^{:key option} [interface/form-element (conj path option)])])

(defmethod interface/component-node-data :heraldry.component/collection-element [path]
  (let [name @(rf/subscribe [:get-value (conj path :name)])
        index (last path)]
    {:title (str (inc index) ": "
                 (if (-> name count pos?)
                   name
                   "<no name>"))}))

(defmethod interface/component-form-data :heraldry.component/collection-element [_path]
  {:form form})
