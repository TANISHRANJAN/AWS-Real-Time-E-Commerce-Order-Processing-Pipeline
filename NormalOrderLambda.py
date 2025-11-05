import json
 
def lambda_handler(event, context):
    # EventBridge Pipes sends a list of records
    if isinstance(event, list):
        records = event
    else:
        records = [event]
 
    for record in records:
        # Extract DynamoDB NewImage
        new_image = record.get('dynamodb', {}).get('NewImage', {})
 
        # Extract fields
        order_id = new_image.get('orderId', {}).get('S')
        status = new_image.get('status', {}).get('S')
        amount_str = new_image.get('amount', {}).get('N')
        email = new_image.get('customerEmail', {}).get('S')
 
        # Skip if amount is missing or not numeric
        try:
            amount = float(amount_str)
        except (TypeError, ValueError):
            print(f"Skipping order {order_id}: invalid amount {amount_str}")
            continue
 
        # Apply numeric filter: only process orders with amount > 100
        if amount <= 100:
            print(f"Skipping order {order_id}: amount {amount} <= 100")
            continue
 
        # Process the order
        print(f"Processing order {order_id} | Status={status} | Amount={amount} | Email={email}")
 
        # Add your business logic here (e.g., notify, update DB, etc.)
 
    return {"statusCode": 200}
