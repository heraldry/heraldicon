(ns heraldicon.frontend.component.entity.collection.element
  (:require
   [clojure.string :as str]
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.drag :as drag]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]))

(defn- form [context]
  (element/elements
   context
   [:name
    :reference]))

(defmethod component/node :heraldicon.entity.collection/element [{:keys [path] :as context}]
  (let [name (interface/get-raw-data (c/++ context :name))
        index (last path)]
    {:title (string/str-tr (inc index) ": "
                           (if (str/blank? name)
                             :string.miscellaneous/no-name
                             name))
     :draggable? true
     :drop-options-fn drag/drop-options
     :drop-fn drag/drop-fn}))

(defmethod component/form :heraldicon.entity.collection/element [_context]
  form)
