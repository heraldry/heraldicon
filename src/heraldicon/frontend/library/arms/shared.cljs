(ns heraldicon.frontend.library.arms.shared
  (:require
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.context :as context]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.history.core :as history]
   [re-frame.core :as rf]))

(def entity-type
  :heraldicon.entity.type/arms)

(history/register-undoable-path (form/data-path entity-type))

(def base-context
  (let [form-db-path (form/data-path entity-type)]
    (assoc
     context/default
     :path form-db-path
     :render-options-path (conj form-db-path :data :achievement :render-options)
     :select-component-fn (fn [context]
                            (rf/dispatch [::tree/select-node (:path context)])))))
