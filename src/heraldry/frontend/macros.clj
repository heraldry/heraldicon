(ns heraldry.frontend.macros)

(defmacro reg-event-db [event-name event-fn]
  `(re-frame.core/reg-event-db ~event-name
     (fn [~'db ~'args]
       (let [~'new-db (~event-fn ~'db ~'args)]
         (heraldry.frontend.history/add-new-states ~'db ~'new-db)))))
