(ns heraldicon.frontend.component.entity.collection.element
  (:require
   [clojure.string :as str]
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.element :as-alias component.element]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [re-frame.core :as rf]))

(defn- form [context]
  (element/elements
   context
   [:name
    :reference]))

(defn drop-options-fn
  [dragged-node-path _dragged-node-type
   drop-node-path _drop-node-type
   _drop-node-open?]
  (let [current-index (last dragged-node-path)
        new-index (last drop-node-path)]

    (when (and (not= dragged-node-path drop-node-path)
               (= (drop-last dragged-node-path)
                  (drop-last drop-node-path)))
      (cond
        (= new-index
           (dec current-index)) #{:above}

        (= new-index
           (inc current-index)) #{:below}

        :else #{:above :below}))))

(defn drop-fn
  [dragged-node-context drop-node-context where]
  (let [new-index (last (:path drop-node-context))
        new-index (case where
                    :above new-index
                    :below (inc new-index))]
    (rf/dispatch [::component.element/move dragged-node-context new-index])))

(defmethod component/node :heraldicon.entity.collection/element [{:keys [path] :as context}]
  (let [name (interface/get-raw-data (c/++ context :name))
        index (last path)]
    {:title (string/str-tr (inc index) ": "
                           (if (str/blank? name)
                             :string.miscellaneous/no-name
                             name))
     :draggable? true
     :drop-options-fn drop-options-fn
     :drop-fn drop-fn}))

(defmethod component/form :heraldicon.entity.collection/element [_context]
  form)
