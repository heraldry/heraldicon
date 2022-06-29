(ns heraldicon.frontend.component.core
  (:require
   [heraldicon.interface :as interface]
   [taoensso.timbre :as log]))

(defmulti node interface/effective-component-type)

(defmethod node nil [context]
  (log/warn :not-implemented "node" context)
  {:title (str "unknown")})

(defmulti form interface/effective-component-type)

(defmethod form nil [_context]
  nil)
