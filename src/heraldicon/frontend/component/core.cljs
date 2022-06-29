(ns heraldicon.frontend.component.core
  (:require
   [heraldicon.interface :as interface]
   [taoensso.timbre :as log]))

(defmulti node-data interface/effective-component-type)

(defmethod node-data nil [context]
  (log/warn :not-implemented "node-data" context)
  {:title (str "unknown")})

(defmulti form-data interface/effective-component-type)

(defmethod form-data nil [_context]
  nil)
