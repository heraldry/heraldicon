(ns heraldicon.frontend.library.ribbon.shared
  (:require
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.history.core :as history]))

(def entity-type
  :heraldicon.entity.type/ribbon)

(history/register-undoable-path (form/data-path entity-type))
