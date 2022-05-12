(ns heraldicon.util.vec)

(defn remove-element
  "Remove element in vector by index."
  [v pos]
  (vec (concat (subvec v 0 pos) (subvec v (inc pos)))))

(defn insert-element
  "Add element in vector by index."
  [v pos el]
  (vec (concat (subvec v 0 pos) [el] (subvec v pos))))

(defn move-element
  "Move element in vector by index."
  [v pos1 pos2]
  (if (= pos1 pos2)
    v
    (vec (insert-element (remove-element v pos1) pos2 (nth v pos1)))))
