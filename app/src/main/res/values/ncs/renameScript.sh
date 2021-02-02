for f in *.png; do
    mv -- "$f" "${f:0:9}.png"
done
