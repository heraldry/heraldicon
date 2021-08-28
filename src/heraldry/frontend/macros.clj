(ns heraldry.frontend.macros
  (:require [re-frame.core :as rf]))

(defmacro reg-event-db [event-name event-fn]
  `(rf/reg-event-db ~event-name
     (fn [~'db ~'args]
       (let [~'new-db (~event-fn ~'db ~'args)]
         (~'heraldry.frontend.undo/add-new-state ~'db ~'new-db)))))
