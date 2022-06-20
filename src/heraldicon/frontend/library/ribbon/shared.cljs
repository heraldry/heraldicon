(ns heraldicon.frontend.library.ribbon.shared
  (:require
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.history.core :as history]))

(def form-id
  :heraldicon.entity/ribbon)

(history/register-undoable-path (form/data-path form-id))
