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

(def base-context
  (let [form-db-path (form/data-path entity-type)]
    (-> context/default
        (c/<< :path form-db-path)
        (c/<< :render-options-path (conj form-db-path :data :achievement :render-options))
        (c/set-render-hint :select-component-fn
                           (fn [context]
                             (let [path (:path context)
                                   path (if @(rf/subscribe [:get (conj path :field)])
                                          (conj path :field)
                                          path)]
                               (rf/dispatch [::tree/select-node path])))))))
