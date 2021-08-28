(ns heraldry.macros
  (:require [re-frame.core :as rf]))

(defmacro reg-event-db [event-name event-fn]
  `(rf/reg-event-db ~event-name
     (fn [~'db ~'args]
       (~event-fn ~'db ~'args))))
