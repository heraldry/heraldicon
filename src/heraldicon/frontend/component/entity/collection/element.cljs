(ns heraldicon.frontend.component.entity.collection.element
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [re-frame.core :as rf]))

(def highlighted-element-path
  [:ui :collection-library :selected-element])

(rf/reg-sub ::highlighted-element
  (fn [_ _]
    (rf/subscribe [:get highlighted-element-path]))

  (fn [value _]
    value))

(rf/reg-sub ::highlighted?
  (fn [_ _]
    (rf/subscribe [:get highlighted-element-path]))

  (fn [value [_ path]]
    (= value path)))

(defn highlight-element [path]
  (rf/dispatch-sync [:set highlighted-element-path path]))

(defn- form [context]
  (element/elements
   context
   [:name
    :reference]))

(defmethod component/node-data :heraldicon.entity.collection/element [{:keys [path] :as context}]
  (let [name (interface/get-raw-data (c/++ context :name))
        index (last path)]
    {:title (string/str-tr (inc index) ": "
                           (if (-> name count pos?)
                             name
                             :string.miscellaneous/no-name))}))

(defmethod component/form :heraldicon.entity.collection/element [_context]
  form)
