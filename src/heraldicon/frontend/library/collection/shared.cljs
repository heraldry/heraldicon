(ns heraldicon.frontend.library.collection.shared
  (:require
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.history.core :as history]))

(def entity-type
  :heraldicon.entity.type/collection)

(history/register-undoable-path (form/data-path entity-type))
