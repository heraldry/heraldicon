(ns heraldicon.util.async
  (:require
   [com.wsscode.async.async-cljs :refer [<? go-catch]]))

;; TODO: reconsider this, probably can be done better, and this might swallow exceptions
(defn wait-for-all [chans]
  (go-catch
   (loop [result []
          [c & rest] chans]
     (if c
       (let [arms (<? c)]
         (recur (conj result arms) rest))
       result))))

(defn promise-from-callback [f]
  (js/Promise. (fn [resolve reject]
                 (f (fn [error data]
                      (if (nil? error)
                        (resolve data)
                        (reject error)))))))
