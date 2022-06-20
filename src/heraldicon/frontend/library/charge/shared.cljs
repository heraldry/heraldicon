(ns heraldicon.frontend.library.charge.shared
  (:require
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.history.core :as history]))

(def form-id
  :heraldicon.entity/charge)

(def form-db-path
  (form/data-path form-id))

(history/register-undoable-path form-db-path)
