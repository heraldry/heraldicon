(ns heraldicon.frontend.blazonry-editor.state)

;; :revision counts changes made by the app rather than by typing, so the editor
;; knows when to write :text and :cursor back into the textarea.
(defn create []
  {:text ""
   :cursor 0
   :error-index nil
   :revision 0})

(defn text [state]
  (:text state))

(defn cursor-index [state]
  (:cursor state))

(defn error-index [state]
  (:error-index state))

(defn revision [state]
  (:revision state))

(defn set-cursor-index [state index]
  (assoc state :cursor (-> index
                           (max 0)
                           (min (count (:text state))))))

(defn update-from-input [state text cursor]
  (-> state
      (assoc :text text)
      (set-cursor-index cursor)))

(defn set-text [state text]
  (-> state
      (assoc :text text)
      (set-cursor-index (count text))
      (update :revision inc)))

(defn replace-text [state from-index to-index text]
  (let [current (:text state)
        from-index (-> from-index (max 0) (min (count current)))
        to-index (-> to-index (max from-index) (min (count current)))]
    (-> state
        (assoc :text (str (subs current 0 from-index) text (subs current to-index)))
        (set-cursor-index (+ from-index (count text)))
        (update :revision inc))))

(defn highlight-unknown-string [state index]
  (assoc state :error-index index))
