(ns heraldry.frontend.ui.form.collection-element
  (:require
   [heraldry.frontend.ui.interface :as ui-interface]
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

(defn form [context]
  (ui-interface/form-elements
   context
   [:name
    :reference]))

(defmethod ui-interface/component-node-data :heraldry.component/collection-element [{:keys [path] :as _context}]
  (let [name @(rf/subscribe [:get-value (conj path :name)])
        index (last path)]
    {:title (util/str-tr (inc index) ": "
                         (if (-> name count pos?)
                           name
                           {:en "<no name>"
                            :de "<unbenannt>"}))}))

(defmethod ui-interface/component-form-data :heraldry.component/collection-element [_context]
  {:form form})
