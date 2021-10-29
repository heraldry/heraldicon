(ns heraldry.frontend.ui.form.collection-element
  (:require
   [heraldry.frontend.ui.interface :as interface]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

(def ui-highlighted-element-path [:ui :collection-library :selected-element])

(rf/reg-sub :collection-library-highlighted-element
  (fn [_ _]
    (rf/subscribe [:get ui-highlighted-element-path]))

  (fn [value _]
    value))

(rf/reg-sub :collection-library-highlighted?
  (fn [_ _]
    (rf/subscribe [:get ui-highlighted-element-path]))

  (fn [value [_ path]]
    (= value path)))

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
    {:title (util/str-tr (inc index) ": "
                         (if (-> name count pos?)
                           name
                           {:en "<no name>"
                            :de "<unbenannt>"}))}))

(defmethod interface/component-form-data :heraldry.component/collection-element [_path]
  {:form form})
