(ns heraldry.frontend.macros)

(defmacro reg-event-db [event-name event-fn]
  `(re-frame.core/reg-event-db ~event-name
     (fn [db# args#]
       (let [new-db# (~event-fn db# args#)
             adjusted-db# (heraldry.frontend.state-hook/handle-db-changes db# new-db#)]
         (heraldry.frontend.history.state/add-new-states db# adjusted-db#)))))
