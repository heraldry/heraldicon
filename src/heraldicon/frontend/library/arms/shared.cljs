(ns heraldicon.frontend.library.arms.shared
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.context :as context]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.history.core :as history]
   [re-frame.core :as rf]))

(def entity-type
  :heraldicon.entity.type/arms)

(history/register-undoable-path (form/data-path entity-type))

(defn- determine-component-path [context]
  (let [path (:path context)
        path (if @(rf/subscribe [:get (conj path :field)])
               (conj path :field)
               path)
        ;; if this is the field of a subfield, then use the path of the subfield,
        ;; because that's the node displayed in the tree
        path (if (= (first (take-last 3 path)) :fields)
               (vec (drop-last path))
               path)]
    path))

(def base-context
  (let [form-db-path (form/data-path entity-type)]
    (-> context/default
        (c/<< :path form-db-path)
        (c/<< :render-options-path (conj form-db-path :data :achievement :render-options))
        (c/set-render-hint :select-component-fn #(rf/dispatch [::tree/select-node (determine-component-path %)])
                           :enter-component-fn #(rf/dispatch [::tree/highlight-node (determine-component-path %)])
                           :leave-component-fn #(rf/dispatch [::tree/unhighlight-node (determine-component-path %)])))))
