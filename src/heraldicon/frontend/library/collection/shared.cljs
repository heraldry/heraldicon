(ns heraldicon.frontend.library.collection.shared
  (:require
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.history.core :as history]))

(def form-id
  :heraldicon.entity/collection)

(history/register-undoable-path (form/data-path form-id))
